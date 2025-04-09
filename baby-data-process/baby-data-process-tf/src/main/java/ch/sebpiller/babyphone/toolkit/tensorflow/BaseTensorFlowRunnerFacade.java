package ch.sebpiller.babyphone.toolkit.tensorflow;

import lombok.extern.slf4j.Slf4j;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.op.Ops;
import org.tensorflow.proto.ConfigProto;
import org.tensorflow.proto.GraphDef;

import java.io.Closeable;
import java.io.FileInputStream;
import java.nio.file.Path;

@Slf4j
public abstract class BaseTensorFlowRunnerFacade implements Closeable, AutoCloseable {
    protected final Graph graph;
    protected final Ops ops;
    protected final Session session;

    protected BaseTensorFlowRunnerFacade(Path modelPath) {
        graph = new Graph();

        if (modelPath != null)
            try (var is = new FileInputStream(modelPath.toFile())) {
                graph.importGraphDef(GraphDef.parseFrom(is));
            } catch (Exception e) {
                log.error("Failed to load model from {}", modelPath, e);
            }

        session = new Session(graph, ConfigProto.newBuilder()
                .setAllowSoftPlacement(true)
                .build());
        ops = Ops.create(graph);
    }

    @Override
    public void close() {
        log.info("Closing TensorFlow session");

        session.close();
        graph.close();
    }

}
