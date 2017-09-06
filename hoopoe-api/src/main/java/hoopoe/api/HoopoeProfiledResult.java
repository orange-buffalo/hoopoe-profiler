package hoopoe.api;

import java.util.ArrayList;
import java.util.Collection;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HoopoeProfiledResult {

    private Collection<HoopoeProfiledInvocationRoot> invocations = new ArrayList<>();

    public HoopoeProfiledResult(Collection<HoopoeProfiledInvocationRoot> invocations) {
        this.invocations = new ArrayList<>(invocations);
    }

    public Collection<HoopoeProfiledInvocationRoot> getInvocations() {
        return new ArrayList<>(invocations);
    }

}
