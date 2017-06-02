package com.baidu.hugegraph.example;

import java.util.Iterator;
import java.util.List;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.hugegraph.HugeFactory;
import com.baidu.hugegraph.HugeGraph;
import com.baidu.hugegraph.backend.BackendException;
import com.baidu.hugegraph.backend.id.IdGeneratorFactory;
import com.baidu.hugegraph.backend.query.ConditionQuery;
import com.baidu.hugegraph.backend.tx.GraphTransaction;
import com.baidu.hugegraph.dist.RegisterUtil;
import com.baidu.hugegraph.schema.SchemaManager;
import com.baidu.hugegraph.type.HugeType;
import com.baidu.hugegraph.type.define.HugeKeys;
import com.baidu.hugegraph.type.schema.VertexLabel;

/**
 * Created by jishilei on 17/3/16.
 */
public class Example1 {

    private static final Logger logger = LoggerFactory.getLogger(Example1.class);

    public static void main(String[] args) throws InterruptedException {

        logger.info("Example1 start!");
        RegisterUtil.registerCore();
        RegisterUtil.registerCassandra();

        String confFile = Example1.class.getClassLoader().getResource("hugegraph.properties").getPath();
        HugeGraph graph = HugeFactory.open(confFile);
        graph.clearBackend();
        graph.initBackend();

        Example1.showFeatures(graph);
        Example1.load(graph);

        Example1.thread(graph);

        graph.close();
        System.exit(0);
    }

    private static void thread(HugeGraph graph) throws InterruptedException {
        Thread t = new Thread(() -> {
            // TODO: add graph.initTx()
            graph.addVertex(T.label, "book", "name", "java-11");
            graph.addVertex(T.label, "book", "name", "java-12");
            // TODO: add graph.destroyTx()
            graph.close(); // close current thread tx/store

            GraphTransaction tx = graph.openTransaction();
            tx.addVertex(T.label, "book", "name", "java-21");
            tx.addVertex(T.label, "book", "name", "java-22");
            tx.close(); // this will cause the schema tx not to be closed!
            graph.close(); // this will close the schema tx
        });

        t.start();
        t.join();
    }

    public static void showFeatures(final HugeGraph graph) {
        logger.info("supportsPersistence : " + graph.features().graph().supportsPersistence());
    }

    public static void load(final HugeGraph graph) {

        /************************* schemaManager operating *************************/
        SchemaManager schema = graph.schema();
        logger.info("===============  propertyKey  ================");
        schema.makePropertyKey("id").asInt().create();
        schema.makePropertyKey("~exist").asText().create();
        schema.makePropertyKey("name").asText().create();
        schema.makePropertyKey("gender").asText().create();
        schema.makePropertyKey("instructions").asText().create();
        schema.makePropertyKey("category").asText().create();
        schema.makePropertyKey("year").asInt().create();
        schema.makePropertyKey("time").asText().create();
        schema.makePropertyKey("timestamp").asTimestamp().create();
        schema.makePropertyKey("ISBN").asText().create();
        schema.makePropertyKey("calories").asInt().create();
        schema.makePropertyKey("amount").asText().create();
        schema.makePropertyKey("stars").asInt().create();
        schema.makePropertyKey("age").asInt().valueSingle().create();
        schema.makePropertyKey("comment").asText().valueSet().create();
        schema.makePropertyKey("contribution").asText().valueSet().create();
        schema.makePropertyKey("nickname").asText().valueList().create();
        schema.makePropertyKey("lived").asText().create();
        schema.makePropertyKey("country").asText().valueSet().properties("livedIn").create();
        schema.makePropertyKey("city").asText().create();
        schema.makePropertyKey("sensor_id").asUuid().create();

        logger.info("===============  vertexLabel  ================");

        VertexLabel person = schema.makeVertexLabel("person")
                .properties("name", "age", "city")
                .primaryKeys("name")
                .create();
        schema.makeVertexLabel("author").properties("id", "name").primaryKeys("id").create();
        schema.makeVertexLabel("language").properties("name").primaryKeys("name").create();
        schema.makeVertexLabel("recipe").properties("name", "instructions").primaryKeys("name").create();
        schema.makeVertexLabel("book").properties("name").primaryKeys("name").create();
        schema.makeVertexLabel("reviewer").properties("name").primaryKeys("name").create();
        // vertex label must have the properties that specified in primary key
        schema.makeVertexLabel("FridgeSensor").properties("city").primaryKeys("city").create();

        logger.info("===============  vertexLabel & index  ================");
        schema.makeIndexLabel("personByCity").on(person).secondary().by("city").create();
        schema.makeIndexLabel("personByAge").on(person).search().by("age").create();
        // schemaManager.vertexLabel("author").index("byName").secondary().by("name").add();
        // schemaManager.vertexLabel("recipe").index("byRecipe").materialized().by("name").add();
        // schemaManager.vertexLabel("meal").index("byMeal").materialized().by("name").add();
        // schemaManager.vertexLabel("ingredient").index("byIngredient").materialized().by("name").add();
        // schemaManager.vertexLabel("reviewer").index("byReviewer").materialized().by("name").add();

        logger.info("===============  edgeLabel  ================");

        schema.makeEdgeLabel("authored").singleTime()
                .link("author", "book")
                .properties("contribution")
                .create();

        schema.makeEdgeLabel("look").multiTimes().properties("time")
                .link("author", "book")
                .link("person", "book")
                .sortKeys("time")
                .create();

        schema.makeEdgeLabel("created").singleTime()
                .link("author", "language")
                .create();

        schema.makeEdgeLabel("rated").link("reviewer", "recipe").create();

        logger.info("===============  schemaManager desc  ================");
        schema.desc().forEach(element -> System.out.println(element.schema()));

        /************************* data operating *************************/

        // Directly into the back-end
        graph.addVertex(T.label, "book", "name", "java-3");

        graph.addVertex(T.label, "person", "name", "Baby",
                "city", "Hongkong", "age", 3);
        graph.addVertex(T.label, "person", "name", "James",
                "city", "Beijing", "age", 19);
        graph.addVertex(T.label, "person", "name", "Tom Cat",
                "city", "Beijing", "age", 20);
        graph.addVertex(T.label, "person", "name", "Lisa",
                "city", "Beijing", "age", 20);
        graph.addVertex(T.label, "person", "name", "Hebe",
                "city", "Taipei", "age", 21);

        // Must commit manually
        GraphTransaction tx = graph.openTransaction();

        logger.info("===============  addVertex  ================");
        Vertex james = tx.addVertex(T.label, "author",
                "id", 1, "name", "James Gosling", "age", 62, "lived", "Canadian");

        Vertex java = tx.addVertex(T.label, "language", "name", "java");
        Vertex book1 = tx.addVertex(T.label, "book", "name", "java-1");
        Vertex book2 = tx.addVertex(T.label, "book", "name", "java-2");
        Vertex book3 = tx.addVertex(T.label, "book", "name", "java-3");

        james.addEdge("created", java);
        james.addEdge("authored", book1,
                "contribution", "1990-1-1",
                "comment", "it's a good book",
                "comment", "it's a good book",
                "comment", "it's a good book too");
        james.addEdge("authored", book2, "contribution", "2017-4-28");

        james.addEdge("look", book2, "time", "2017-4-28");
        james.addEdge("look", book3, "time", "2016-1-1");
        james.addEdge("look", book3, "time", "2017-4-28");

        // commit data changes
        try {
            tx.commit();
        } catch (BackendException e) {
            e.printStackTrace();
            try {
                tx.rollback();
            } catch (BackendException e2) {
                // TODO Auto-generated catch block
                e2.printStackTrace();
            }
        }

        // use the default Transaction to commit
        graph.addVertex(T.label, "book", "name", "java-3");

        // tinkerpop tx
        graph.tx().open();
        graph.addVertex(T.label, "book", "name", "java-4");
        graph.addVertex(T.label, "book", "name", "java-5");
        graph.tx().commit();
        graph.tx().close();

        // query all
        GraphTraversal<Vertex, Vertex> vertexes = graph.traversal().V();
        int size = vertexes.toList().size();
        assert size == 12;
        System.out.println(">>>> query all vertices: size=" + size);

        // query vertex by primary-values
        vertexes = graph.traversal().V().hasLabel("author").has("id", "1");
        List<Vertex> vertexList = vertexes.toList();
        assert vertexList.size() == 1;
        System.out.println(">>>> query vertices by primary-values: " + vertexList);

        // query vertex by id and query out edges
        vertexes = graph.traversal().V("author\u00021");
        GraphTraversal<Vertex, Edge> edgesOfVertex = vertexes.outE("created");
        List<Edge> edgeList = edgesOfVertex.toList();
        assert edgeList.size() == 1;
        System.out.println(">>>> query edges of vertex: " + edgeList);

        vertexes = graph.traversal().V("author\u00021");
        GraphTraversal<Vertex, Vertex> verticesOfVertex = vertexes.out("created");
        vertexList = verticesOfVertex.toList();
        assert vertexList.size() == 1;
        System.out.println(">>>> query vertices of vertex: " + vertexList);

        // query edge by sort-values
        vertexes = graph.traversal().V("author\u00021");
        edgesOfVertex = vertexes.outE("look").has("time", "2017-4-28");
        edgeList = edgesOfVertex.toList();
        assert edgeList.size() == 2;
        System.out.println(">>>> query edges of vertex by sort-values: " + edgeList);

        // query vertex by condition (filter by property name)
        ConditionQuery q = new ConditionQuery(HugeType.VERTEX);
        q.query(IdGeneratorFactory.generator().generate("author\u00021"));
        // TODO: remove the PROPERTIES which may just be used by Cassandra
        q.hasKey(HugeKeys.PROPERTIES, "age");

        Iterator<Vertex> vertices = graph.vertices(q);
        assert vertices.hasNext();
        System.out.println(">>>> queryVertices(age): " + vertices.hasNext());
        while (vertices.hasNext()) {
            System.out.println(">>>> queryVertices(age): " + vertices.next());
        }

        // query all edges
        GraphTraversal<Edge, Edge> edges = graph.traversal().E().limit(2);
        size = edges.toList().size();
        assert size == 2;
        System.out.println(">>>> query all edges with limit 2: size=" + size);

        // query edge by id
        String id = "author\u00021\u0001authored\u0001\u0001book\u0002java-2";
        edges = graph.traversal().E(id);
        edgeList = edges.toList();
        assert edgeList.size() == 1;
        System.out.println(">>>> query edge by id: " + edgeList);

        Edge edge = graph.traversal().E(id).toList().get(0);
        edges = graph.traversal().E(edge.id());
        edgeList = edges.toList();
        assert edgeList.size() == 1;
        System.out.println(">>>> query edge by id: " + edgeList);

        // query edge by condition
        q = new ConditionQuery(HugeType.EDGE);
        q.eq(HugeKeys.SOURCE_VERTEX, "author\u00021");
        q.eq(HugeKeys.DIRECTION, Direction.OUT);
        q.eq(HugeKeys.LABEL, "authored");
        q.eq(HugeKeys.SORT_VALUES, "");
        q.eq(HugeKeys.TARGET_VERTEX, "book\u0002java-1");
        // NOTE: query edge by has-key just supported by Cassandra
        // q.hasKey(HugeKeys.PROPERTIES, "contribution");

        Iterator<Edge> edges2 = graph.edges(q);
        assert edges2.hasNext();
        System.out.println(">>>> queryEdges(contribution): " + edges2.hasNext());
        while (edges2.hasNext()) {
            System.out.println(">>>> queryEdges(contribution): " + edges2.next());
        }

        // query by vertex label
        vertexes = graph.traversal().V().hasLabel("book");
        size = vertexes.toList().size();
        assert size == 5;
        System.out.println(">>>> query all books: size=" + size);

        // query by vertex label and key-name
        vertexes = graph.traversal().V().hasLabel("person").has("age");
        size = vertexes.toList().size();
        assert size == 5;
        System.out.println(">>>> query all persons with age: size=" + size);

        // query by vertex props
        vertexes = graph.traversal().V().hasLabel("person").has("city", "Taipei");
        vertexList = vertexes.toList();
        assert vertexList.size() == 1;
        System.out.println(">>>> query all persons in Taipei: " + vertexList);

        vertexes = graph.traversal().V().hasLabel("person").has("age", 19);
        vertexList = vertexes.toList();
        assert vertexList.size() == 1;
        System.out.println(">>>> query all persons age==19: " + vertexList);

        vertexes = graph.traversal().V().hasLabel("person").has("age", P.lt(19));
        vertexList = vertexes.toList();
        assert vertexList.size() == 1;
        assert vertexList.get(0).property("age").value().equals(3);
        System.out.println(">>>> query all persons age<19: " + vertexList);

        // remove vertex (and its edges)
        vertexes = graph.traversal().V().hasLabel("person").has("age", 19);
        Vertex vertex = vertexes.toList().get(0);
        vertex.addEdge("look", book3, "time", "2017-5-3");
        System.out.println(">>>> remove vertex: " + vertex);
        vertex.remove();
        try {
            graph.traversal().V(vertex.id()).toList();
            assert false;
        } catch (BackendException e) {
            assert e.getMessage().contains("Not found id");
        }

        // remove edge
        id = "author\u00021\u0001authored\u0001\u0001book\u0002java-2";
        edges = graph.traversal().E(id);
        edge = edges.toList().get(0);
        System.out.println(">>>> remove edge: " + edge);
        edge.remove();
        try {
            graph.traversal().E(id).toList();
            assert false;
        } catch (BackendException e) {
            assert e.getMessage().contains("Not found id");
        }
    }

}
