package hoopoe.api;

import lombok.Getter;

public class HoopoeAttribute {

    @Getter
    private String name;

    @Getter
    private String details;

    @Getter
    private boolean contributingTime;

    public HoopoeAttribute(String name, String details, boolean contributingTime) {
        this.name = name;
        this.details = details;
        this.contributingTime = contributingTime;
    }
}
