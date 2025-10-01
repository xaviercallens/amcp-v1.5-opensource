const assert = require('assert');
const { CorrelationTrackingManager } = require('io.amcp.connectors.ai.correlation');

describe('CorrelationTrackingManager', () => {
    it('should create correlation with valid parameters', async () => {
        const result = await CorrelationTrackingManager.createCorrelation('testId', 'testType', new Map());
        assert.strictEqual(result instanceof Map, true);
    });

    it('should throw error for invalid parameters', async () => {
        try {
            await CorrelationTrackingManager.createCorrelation('testId', 123);
        } catch (error) {
            assert.strictEqual(error.message, 'Invalid parameters');
        }
    });
});