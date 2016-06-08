package hoopoe.core.bootstrap;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PluginActionIndicies implements Serializable {
    private int[] ids;
}
