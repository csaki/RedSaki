package com.devsaki.redsaki.dto;

/**
 * Created by DevSaki on 06/11/2016.
 */

public class SubredditDTO {

    private String id;
    private String displayName;
    private String url;
    private String publicDescription;
    private boolean suscriber;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPublicDescription() {
        return publicDescription;
    }

    public void setPublicDescription(String publicDescription) {
        this.publicDescription = publicDescription;
    }

    public boolean isSuscriber() {
        return suscriber;
    }

    public void setSuscriber(boolean suscriber) {
        this.suscriber = suscriber;
    }
}
