package com.bpanda.keycloak.model;

public class UserSyncResult {
    private boolean ignored;
    private String status;
    private int added;
    private int updated;
    private int removed;
    private int failed;

    public UserSyncResult() {
    }


    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAdded() {
        return added;
    }

    public void setAdded(int added) {
        this.added = added;
    }

    public int getUpdated() {
        return updated;
    }

    public void setUpdated(int updated) {
        this.updated = updated;
    }

    public int getRemoved() {
        return removed;
    }

    public void setRemoved(int removed) {
        this.removed = removed;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }


    @Override
    public String toString() {
        return "UserSyncResult{" +
                "status='" + status + '\'' +
                '}';
    }

    public boolean hasChanges() {
        return (getAdded() + getRemoved() + getUpdated()) > 0;
    }
}
