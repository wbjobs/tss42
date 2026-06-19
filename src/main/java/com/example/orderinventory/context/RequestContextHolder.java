package com.example.orderinventory.context;

public class RequestContextHolder {

    private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<>();

    public static void setContext(RequestContext context) {
        CONTEXT.set(context);
    }

    public static RequestContext getContext() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static class RequestContext {
        private Long userId;
        private String ip;
        private String deviceFingerprint;
        private Integer riskScore;
        private Integer riskFlag;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getDeviceFingerprint() {
            return deviceFingerprint;
        }

        public void setDeviceFingerprint(String deviceFingerprint) {
            this.deviceFingerprint = deviceFingerprint;
        }

        public Integer getRiskScore() {
            return riskScore;
        }

        public void setRiskScore(Integer riskScore) {
            this.riskScore = riskScore;
        }

        public Integer getRiskFlag() {
            return riskFlag;
        }

        public void setRiskFlag(Integer riskFlag) {
            this.riskFlag = riskFlag;
        }
    }
}
