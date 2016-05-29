package hoopoe.api;

import lombok.Getter;

public class HoopoeAttributeSummary {

    @Getter
    private String name;

    @Getter
    private boolean contributingTime;

    @Getter
    private long totalTimeInNs;

    @Getter
    private int totalOccurrences;

    public HoopoeAttributeSummary(String name,
                                  boolean contributingTime,
                                  long totalTimeInNs,
                                  int totalOccurrences) {
        this.name = name;
        this.contributingTime = contributingTime;
        this.totalTimeInNs = totalTimeInNs;
        this.totalOccurrences = totalOccurrences;
    }
}
