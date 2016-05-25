package hoopoe.test.core.supplements;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HoopoeTestHelper {

    public static long msToNs(long ms) {
        return ms * 1_000_000;
    }

}
