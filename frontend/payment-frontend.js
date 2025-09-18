/**
 * Payment Frontend Implementation
 * Handles payment creation, real-time updates, and error management
 */

class PaymentService {
    constructor() {
        this.baseUrl = 'http://localhost:8081/api/payments';
        this.stompClient = null;
        this.isConnected = false;
        this.retryCount = 0;
        this.maxRetries = 3;
        this.retryDelay = 1000; // 1 second
    }

    /**
     * Create a new payment
     */
    async createPayment(paymentData) {
        try {
            const response = await fetch(`${this.baseUrl}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(paymentData)
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || `HTTP ${response.status}`);
            }

            const result = await response.json();
            
            // Connect to WebSocket for real-time updates
            this.connectWebSocket(result.id, paymentData.userId);
            
            return result;
        } catch (error) {
            console.error('Payment creation failed:', error);
            throw this.handleError(error);
        }
    }

    /**
     * Connect to WebSocket for real-time updates
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
     * Handle payment status updates from WebSocket
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
                break;
            case 'FAILED':
                this.showErrorStatus(payment);
                break;
            default:
                console.log('Unknown payment status:', payment.status);
        }
    }

    /**
     * Get payment status by ID
     */
    async getPaymentStatus(paymentId) {
        try {
            const response = await fetch(`${this.baseUrl}/status/${paymentId}`);
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('Failed to get payment status:', error);
            throw this.handleError(error);
        }
    }

    /**
     * Get user's payment history
     */
    async getUserPayments(userId) {
        try {
            const response = await fetch(`${this.baseUrl}/user/${userId}`);
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('Failed to get user payments:', error);
            throw this.handleError(error);
        }
    }

    /**
     * Get payment statistics
     */
    async getPaymentStats() {
        try {
            const response = await fetch(`${this.baseUrl}/stats`);
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('Failed to get payment stats:', error);
            throw this.handleError(error);
        }
    }

    /**
     * Health check
     */
    async healthCheck() {
        try {
            const response = await fetch(`${this.baseUrl}/health`);
            return response.ok;
        } catch (error) {
            console.error('Health check failed:', error);
            return false;
        }
    }

    /**
     * Handle WebSocket connection errors with retry logic
     */
    handleWebSocketError() {
        if (this.retryCount < this.maxRetries) {
            this.retryCount++;
            console.log(`Retrying WebSocket connection (${this.retryCount}/${this.maxRetries})...`);
            
            setTimeout(() => {
                this.connectWebSocket();
            }, this.retryDelay * this.retryCount);
        } else {
            console.error('Max WebSocket retry attempts reached');
            this.showConnectionError();
        }
    }

    /**
     * Handle different types of errors
     */
    handleError(error) {
        if (error.message.includes('Failed to fetch')) {
            return {
                type: 'NETWORK_ERROR',
                message: 'Unable to connect to payment service. Please check your internet connection.',
                retryable: true
            };
        } else if (error.message.includes('HTTP 503')) {
            return {
                type: 'SERVICE_UNAVAILABLE',
                message: 'Payment service is temporarily unavailable. Please try again later.',
                retryable: true
            };
        } else if (error.message.includes('HTTP 500')) {
            return {
                type: 'SERVER_ERROR',
                message: 'Internal server error. Please contact support if the problem persists.',
                retryable: false
            };
        } else if (error.message.includes('HTTP 400')) {
            return {
                type: 'VALIDATION_ERROR',
                message: 'Invalid payment data. Please check your input and try again.',
                retryable: false
            };
        } else {
            return {
                type: 'UNKNOWN_ERROR',
                message: error.message || 'An unexpected error occurred.',
                retryable: true
            };
        }
    }

    /**
     * UI Update Methods
     */
    showProcessingStatus(payment) {
        // Update UI to show processing status
        document.getElementById('payment-status').textContent = 'Processing...';
        document.getElementById('payment-status').className = 'status processing';
    }

    showSuccessStatus(payment) {
        // Update UI to show success status
        document.getElementById('payment-status').textContent = 'Payment Completed!';
        document.getElementById('payment-status').className = 'status success';
        document.getElementById('payment-amount').textContent = `$${payment.amount}`;
    }

    showErrorStatus(payment) {
        // Update UI to show error status
        document.getElementById('payment-status').textContent = 'Payment Failed';
        document.getElementById('payment-status').className = 'status error';
        document.getElementById('error-message').textContent = payment.errorMessage || 'Payment processing failed';
    }

    showConnectionError() {
        // Show connection error to user
        document.getElementById('connection-status').textContent = 'Connection lost. Retrying...';
        document.getElementById('connection-status').className = 'connection-error';
    }

    /**
     * Disconnect WebSocket
     */
    disconnect() {
        if (this.stompClient && this.isConnected) {
            this.stompClient.deactivate();
            this.isConnected = false;
        }
    }
}

// Usage Example
const paymentService = new PaymentService();

// Create payment
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
        console.log('Payment created:', result);
    } catch (error) {
        console.error('Payment creation failed:', error);
        
        // Handle different error types
        if (error.retryable) {
            // Show retry button
            showRetryOption();
        } else {
            // Show error message
            showErrorMessage(error.message);
        }
    }
}

// Health check on page load
window.addEventListener('load', async () => {
    const isHealthy = await paymentService.healthCheck();
    if (!isHealthy) {
        document.getElementById('service-status').textContent = 'Service Unavailable';
        document.getElementById('service-status').className = 'service-down';
    }
});

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
    paymentService.disconnect();
});
