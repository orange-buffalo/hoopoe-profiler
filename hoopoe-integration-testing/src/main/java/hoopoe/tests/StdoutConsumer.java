package hoopoe.tests;

import java.util.function.Consumer;
import org.testcontainers.containers.output.OutputFrame;

/**
 * Writes container output to {@link System#out} without modification.
 * <p>
 * To be used in {@link org.testcontainers.containers.Container#withLogConsumer(Consumer)} when container already
 * produces output from loggers, to avoid duplication of loggers decorators.
 */
public class StdoutConsumer implements Consumer<OutputFrame> {

    @Override
    public void accept(OutputFrame outputFrame) {
        if (outputFrame != null) {
            String utf8String = outputFrame.getUtf8String();

            if (utf8String != null) {
                OutputFrame.OutputType outputType = outputFrame.getType();
                String message = utf8String.trim();

                switch (outputType) {
                    case END:
                        break;
                    case STDOUT:
                    case STDERR:
                        System.out.println(message);
                        System.out.flush();
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected outputType " + outputType);
                }
            }
        }
    }
}
