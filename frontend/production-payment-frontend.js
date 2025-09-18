/**
 * Production Payment Frontend
 * Implements strategies used by real payment companies like Dojo, Stripe, PayPal
 * Handles Kafka failures, circuit breakers, and graceful degradation
 */

class ProductionPaymentService {
    constructor() {
        this.baseUrl = 'http://localhost:8081/api/payments';
        this.stompClient = null;
        this.isConnected = false;
        this.retryCount = 0;
        this.maxRetries = 3;
        this.retryDelay = 1000;
        this.circuitBreakerOpen = false;
        this.healthCheckInterval = null;
        this.localPaymentQueue = [];
        this.isOnline = navigator.onLine;
        
        this.initializeService();
    }

    /**
     * Initialize service with health monitoring
     */
    async initializeService() {
        // Start health monitoring
        this.startHealthMonitoring();
        
        // Listen for online/offline events
        window.addEventListener('online', () => this.handleOnline());
        window.addEventListener('offline', () => this.handleOffline());
        
        // Initial health check
        await this.performHealthCheck();
    }

    /**
     * Create payment with production-grade error handling
     */
    async createPayment(paymentData) {
        try {
            // Check if service is healthy
            if (!await this.isServiceHealthy()) {
                return this.handleServiceUnavailable(paymentData);
            }

            const response = await this.makeRequest('POST', '', paymentData);
            
            if (response.status === 'PENDING') {
                // Connect to WebSocket for real-time updates
                this.connectWebSocket(response.id, paymentData.userId);
                
                // Store payment locally for offline handling
                this.storePaymentLocally(response.id, paymentData);
                
                return response;
            } else {
                throw new Error(response.message || 'Payment creation failed');
            }
            
        } catch (error) {
            console.error('Payment creation failed:', error);
            return this.handlePaymentError(paymentData, error);
        }
    }

    /**
     * Make HTTP request with retry logic and circuit breaker
     */
    async makeRequest(method, endpoint, data = null) {
        const url = `${this.baseUrl}${endpoint}`;
        const options = {
            method,
            headers: {
                'Content-Type': 'application/json',
            }
        };

        if (data) {
            options.body = JSON.stringify(data);
        }

        for (let attempt = 1; attempt <= this.maxRetries; attempt++) {
            try {
                const response = await fetch(url, options);
                
                if (response.ok) {
                    this.resetRetryCount();
                    return await response.json();
                } else if (response.status === 503) {
                    // Service unavailable - activate circuit breaker
                    this.activateCircuitBreaker();
                    throw new Error('Service temporarily unavailable');
                } else if (response.status >= 500) {
                    // Server error - retry
                    if (attempt < this.maxRetries) {
                        await this.delay(this.retryDelay * attempt);
                        continue;
                    }
                }
                
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || `HTTP ${response.status}`);
                
            } catch (error) {
                if (attempt === this.maxRetries) {
                    throw error;
                }
                
                // Exponential backoff
                await this.delay(this.retryDelay * Math.pow(2, attempt - 1));
            }
        }
    }

    /**
     * Connect to WebSocket with reconnection logic
     */
    connectWebSocket(paymentId, userId) {
        try {
            const socket = new SockJS(`${this.baseUrl.replace('/api/payments', '')}/ws`);
            this.stompClient = new StompJs.Client({ webSocketFactory: () => socket });

            this.stompClient.onConnect = (frame) => {
                console.log('Connected to WebSocket:', frame);
                this.isConnected = true;
                this.retryCount = 0;
                
                // Subscribe to user-specific payment updates
                this.stompClient.subscribe(`/topic/payments/${userId}`, (message) => {
                    const payment = JSON.parse(message.body);
                    this.handlePaymentUpdate(payment);
                });
            };

            this.stompClient.onStompError = (error) => {
                console.error('WebSocket STOMP error:', error);
                this.handleWebSocketError();
            };

            this.stompClient.onWebSocketClose = (event) => {
                console.log('WebSocket closed:', event);
                this.isConnected = false;
                this.handleWebSocketError();
            };

            this.stompClient.activate();
        } catch (error) {
            console.error('WebSocket connection failed:', error);
            this.handleWebSocketError();
        }
    }

    /**
     * Handle payment status updates
     */
    handlePaymentUpdate(payment) {
        console.log('Payment update received:', payment);
        
        // Update UI based on payment status
        switch (payment.status) {
            case 'PROCESSING':
                this.showProcessingStatus(payment);
                break;
            case 'COMPLETED':
                this.showSuccessStatus(payment);
                this.removeFromLocalQueue(payment.id);
                break;
            case 'FAILED':
                this.showErrorStatus(payment);
                this.removeFromLocalQueue(payment.id);
                break;
            default:
                console.log('Unknown payment status:', payment.status);
        }
    }

    /**
     * Health monitoring (like Stripe's health checks)
     */
    startHealthMonitoring() {
        this.healthCheckInterval = setInterval(async () => {
            await this.performHealthCheck();
        }, 30000); // Check every 30 seconds
    }

    /**
     * Perform health check
     */
    async performHealthCheck() {
        try {
            const healthData = await this.makeRequest('GET', '/health/detailed');
            
            if (healthData.status === 'HEALTHY') {
                this.circuitBreakerOpen = false;
                this.updateServiceStatus('healthy');
            } else if (healthData.status === 'DEGRADED') {
                this.updateServiceStatus('degraded', healthData);
            } else {
                this.activateCircuitBreaker();
                this.updateServiceStatus('unhealthy');
            }
            
        } catch (error) {
            console.error('Health check failed:', error);
            this.activateCircuitBreaker();
            this.updateServiceStatus('unhealthy');
        }
    }

    /**
     * Check if service is healthy
     */
    async isServiceHealthy() {
        if (this.circuitBreakerOpen) {
            return false;
        }
        
        try {
            const response = await fetch(`${this.baseUrl}/health`, { 
                method: 'GET',
                timeout: 5000 
            });
            return response.ok;
        } catch (error) {
            return false;
        }
    }

    /**
     * Handle service unavailable scenario
     */
    handleServiceUnavailable(paymentData) {
        // Store payment locally for later processing
        const localPayment = {
            id: this.generateLocalId(),
            ...paymentData,
            status: 'PENDING',
            timestamp: new Date().toISOString(),
            local: true
        };
        
        this.storePaymentLocally(localPayment.id, localPayment);
        
        return {
            id: localPayment.id,
            status: 'PENDING',
            message: 'Payment queued for processing when service is available',
            local: true
        };
    }

    /**
     * Handle payment errors with user-friendly messages
     */
    handlePaymentError(paymentData, error) {
        let userMessage;
        let retryable = true;
        
        if (error.message.includes('Service temporarily unavailable')) {
            userMessage = 'Payment service is temporarily unavailable. Your payment will be processed when service is restored.';
        } else if (error.message.includes('timeout')) {
            userMessage = 'Payment processing timed out. Please try again.';
        } else if (error.message.includes('network')) {
            userMessage = 'Network error. Please check your connection and try again.';
        } else if (error.message.includes('validation')) {
            userMessage = 'Invalid payment data. Please check your input.';
            retryable = false;
        } else {
            userMessage = 'Payment processing failed. Please try again later.';
        }
        
        return {
            id: null,
            status: 'FAILED',
            message: userMessage,
            retryable: retryable,
            error: error.message
        };
    }

    /**
     * Handle online event
     */
    async handleOnline() {
        console.log('Connection restored');
        this.isOnline = true;
        this.updateConnectionStatus('online');
        
        // Retry queued payments
        await this.retryQueuedPayments();
        
        // Reconnect WebSocket
        if (!this.isConnected) {
            this.connectWebSocket();
        }
    }

    /**
     * Handle offline event
     */
    handleOffline() {
        console.log('Connection lost');
        this.isOnline = false;
        this.updateConnectionStatus('offline');
        
        // Disconnect WebSocket
        if (this.stompClient && this.isConnected) {
            this.stompClient.deactivate();
            this.isConnected = false;
        }
    }

    /**
     * Retry queued payments when connection is restored
     */
    async retryQueuedPayments() {
        const queuedPayments = this.getQueuedPayments();
        
        for (const payment of queuedPayments) {
            try {
                if (payment.local) {
                    // Retry local payment
                    const result = await this.createPayment(payment);
                    if (result.status !== 'FAILED') {
                        this.removeFromLocalQueue(payment.id);
                    }
                }
            } catch (error) {
                console.error('Failed to retry payment:', payment.id, error);
            }
        }
    }

    /**
     * Store payment locally for offline handling
     */
    storePaymentLocally(paymentId, paymentData) {
        const localPayment = {
            id: paymentId,
            ...paymentData,
            timestamp: new Date().toISOString()
        };
        
        this.localPaymentQueue.push(localPayment);
        localStorage.setItem('paymentQueue', JSON.stringify(this.localPaymentQueue));
    }

    /**
     * Get queued payments from local storage
     */
    getQueuedPayments() {
        const stored = localStorage.getItem('paymentQueue');
        if (stored) {
            this.localPaymentQueue = JSON.parse(stored);
        }
        return this.localPaymentQueue;
    }

    /**
     * Remove payment from local queue
     */
    removeFromLocalQueue(paymentId) {
        this.localPaymentQueue = this.localPaymentQueue.filter(p => p.id !== paymentId);
        localStorage.setItem('paymentQueue', JSON.stringify(this.localPaymentQueue));
    }

    /**
     * Activate circuit breaker
     */
    activateCircuitBreaker() {
        this.circuitBreakerOpen = true;
        console.log('Circuit breaker activated');
        
        // Reset circuit breaker after timeout
        setTimeout(() => {
            this.circuitBreakerOpen = false;
            console.log('Circuit breaker reset');
        }, 60000); // 1 minute
    }

    /**
     * Update service status in UI
     */
    updateServiceStatus(status, data = null) {
        const statusElement = document.getElementById('service-status');
        if (statusElement) {
            statusElement.textContent = this.getStatusMessage(status, data);
            statusElement.className = `status ${status}`;
        }
    }

    /**
     * Update connection status in UI
     */
    updateConnectionStatus(status) {
        const connectionElement = document.getElementById('connection-status');
        if (connectionElement) {
            connectionElement.textContent = status === 'online' ? 'Connected' : 'Offline';
            connectionElement.className = `connection ${status}`;
        }
    }

    /**
     * Get status message
     */
    getStatusMessage(status, data) {
        switch (status) {
            case 'healthy':
                return 'Service is healthy';
            case 'degraded':
                return `Service degraded - ${data?.queuedPayments || 0} payments queued`;
            case 'unhealthy':
                return 'Service unavailable - payments will be queued';
            default:
                return 'Unknown status';
        }
    }

    /**
     * Generate local payment ID
     */
    generateLocalId() {
        return 'local_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    }

    /**
     * Delay utility
     */
    delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    /**
     * Reset retry count
     */
    resetRetryCount() {
        this.retryCount = 0;
    }

    /**
     * Cleanup
     */
    destroy() {
        if (this.healthCheckInterval) {
            clearInterval(this.healthCheckInterval);
        }
        
        if (this.stompClient && this.isConnected) {
            this.stompClient.deactivate();
        }
    }
}

// Usage Example
const paymentService = new ProductionPaymentService();

// Create payment with production error handling
async function createPayment() {
    const paymentData = {
        userId: 'user123',
        merchantId: 'merchant456',
        amount: 100.50,
        currency: 'USD',
        description: 'Online purchase'
    };

    try {
        const result = await paymentService.createPayment(paymentData);
        
        if (result.local) {
            showMessage('Payment queued for processing when service is available', 'info');
        } else if (result.status === 'PENDING') {
            showMessage('Payment created successfully', 'success');
        } else {
            showMessage(result.message, 'error');
        }
        
    } catch (error) {
        showMessage('Payment creation failed: ' + error.message, 'error');
    }
}

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
    paymentService.destroy();
});
