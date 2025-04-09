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
        if (modelPath == null) {
            graph = null;
            ops = null;
            session = null;
        } else {
            graph = new Graph();
            try (var is = new FileInputStream(modelPath.toFile())) {
                graph.importGraphDef(GraphDef.parseFrom(is));
            } catch (Exception e) {
                log.error("Failed to load model from {}", modelPath, e);
            }
            session = new Session(graph, ConfigProto.getDefaultInstance().toBuilder()
                    .setAllowSoftPlacement(true)
                    .setLogDevicePlacement(true)
                    .setUsePerSessionThreads(true)
//                .setGraphOptions(GraphOptions.newBuilder()
//                        .setOptimizerOptions(OptimizerOptions.getDefaultInstance().toBuilder()
//                                .setDoFunctionInlining(true)
////                                .setGlobalJitLevelValue(12)
////                                .setOptLevelValue(234)
//                                .build())
//                        .setPlacePrunedGraph(true)
//                        .setEnableBfloat16Sendrecv(true)
//                        .setInferShapes(true)
//                )
                    .build());
            ops = Ops.create(graph);
        }
    }

    @Override
    public void close() {
        log.debug("Closing TensorFlow session");
        if (session != null)
            session.close();
        if (graph != null)
            graph.close();
    }

}
