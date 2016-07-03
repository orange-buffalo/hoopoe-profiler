package hoopoe.api;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HoopoeProfiledInvocationSummary {

    @Getter
    private String threadName;

    @Getter
    private ZonedDateTime profiledOn;

    @Getter
    private String id;

    @Getter
    private long totalTimeInNs;

    private Collection<HoopoeAttributeSummary> attributeSummaries;

    public HoopoeProfiledInvocationSummary(String threadName,
                                           ZonedDateTime profiledOn,
                                           String id,
                                           long totalTimeInNs,
                                           Collection<HoopoeAttributeSummary> attributeSummaries) {
        this.threadName = threadName;
        this.profiledOn = profiledOn;
        this.id = id;
        this.totalTimeInNs = totalTimeInNs;
        this.attributeSummaries = new ArrayList<>(attributeSummaries);
    }

    public Collection<HoopoeAttributeSummary> getAttributeSummaries() {
        return new ArrayList<>(attributeSummaries);
    }
}
