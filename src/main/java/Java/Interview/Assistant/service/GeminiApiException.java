package Java.Interview.Assistant.service;

public class GeminiApiException extends RuntimeException {
    private final int upstreamStatus;
    private final String safeDetail;

    public GeminiApiException(int upstreamStatus, String safeDetail) {
        super(safeDetail);
        this.upstreamStatus = upstreamStatus;
        this.safeDetail = safeDetail;
    }

    public GeminiApiException(int upstreamStatus, String safeDetail, Throwable cause) {
        super(safeDetail, cause);
        this.upstreamStatus = upstreamStatus;
        this.safeDetail = safeDetail;
    }

    public int getUpstreamStatus() {
        return upstreamStatus;
    }

    public String getSafeDetail() {
        return safeDetail;
    }
}
