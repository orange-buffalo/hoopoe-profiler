package hoopoe.api;

import java.util.Collection;

public interface HoopoeHasAttributes {

    void addAttributes(Collection<HoopoeAttribute> attributes);

    Collection<HoopoeAttribute> getAttributes();

}
