package hoopoe.api;

import lombok.Getter;

public class HoopoeAttributeSummary extends HoopoeAttribute {

    @Getter
    private long totalTimeInNs;

    @Getter
    private int totalOccurrences;

    public HoopoeAttributeSummary(String name,
                                  String details,
                                  boolean contributingTime,
                                  long totalTimeInNs,
                                  int totalOccurrences) {
        super(name, details, contributingTime);
        this.totalTimeInNs = totalTimeInNs;
        this.totalOccurrences = totalOccurrences;
    }
}
