package com.nfl.glitr.relay;

import com.nfl.glitr.registry.TypeRegistry;
import graphql.relay.*;
import graphql.schema.*;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static graphql.Assert.assertNotNull;

public class RelayHelper {

    private Relay relay;
    private static final String DUMMY_CURSOR_PREFIX = "simple-cursor";

    private final TypeRegistry typeRegistry;


    public RelayHelper(Relay relay, TypeRegistry typeRegistry) {
        assertNotNull(typeRegistry, "TypeRegistry can't be null");
        assertNotNull(typeRegistry.getNodeInterface(), "NodeInterface can't be null");
        this.relay = relay;
        this.typeRegistry = typeRegistry;
    }

    public GraphQLInterfaceType getNodeInterface() {
        return typeRegistry.getNodeInterface();
    }

    public List<GraphQLArgument> getConnectionFieldArguments() {
        return relay.getConnectionFieldArguments();
    }

    public GraphQLObjectType edgeType(String simpleName, GraphQLOutputType edgeGraphQLOutputType,
                                             GraphQLInterfaceType nodeInterface, List<GraphQLFieldDefinition> graphQLFieldDefinitions) {
        return relay.edgeType(simpleName, edgeGraphQLOutputType, nodeInterface, graphQLFieldDefinitions);
    }

    public GraphQLObjectType connectionType(String simpleName, GraphQLObjectType edgeType, List<GraphQLFieldDefinition> graphQLFieldDefinitions) {
        return relay.connectionType(simpleName, edgeType, graphQLFieldDefinitions);
    }

    public static graphql.relay.Connection buildConnection(Iterable<?> col, int skipItems, int totalCount) {
        List<Edge<Object>> edges = new ArrayList<>();
        int ix = skipItems;

        for (Object object : col) {
            edges.add(new DefaultEdge<>(object, new DefaultConnectionCursor(createCursor(ix++))));
        }

        ConnectionCursor startCursor = null;
        ConnectionCursor endCursor = null ;
        ConnectionCursor previousPageStartCursor = null ;

        boolean hasPreviousPage = skipItems > 0 && totalCount > 0;
        boolean hasNextPage = skipItems + edges.size() + 1 < totalCount;

        if (!edges.isEmpty()) {
            Edge firstEdge = edges.get(0);
            Edge lastEdge = edges.get(edges.size() - 1);
            startCursor = firstEdge.getCursor();
            endCursor = lastEdge.getCursor();

            int offsetFromCursor = getOffsetFromCursor(startCursor.getValue(), 0);

            String cursor = createCursor(0);
            if (offsetFromCursor - edges.size() > 0) {
                cursor = createCursor(offsetFromCursor - edges.size());
            }

            previousPageStartCursor = new DefaultConnectionCursor(cursor);
        }

        PageInfoWithTotal pageInfoWithTotal = new PageInfoWithTotal(startCursor, endCursor, hasPreviousPage, hasNextPage);
        pageInfoWithTotal.setTotal(totalCount);
        pageInfoWithTotal.setPreviousPageStartCursor(previousPageStartCursor);

        return new DefaultConnection<>(edges, pageInfoWithTotal);
    }

    public static String createCursor(int offset) {
        return Base64.toBase64(DUMMY_CURSOR_PREFIX + offset);
    }

    public static int getOffsetFromCursor(String cursor, int defaultValue) {
        if (cursor == null) return defaultValue;
        String string = Base64.fromBase64(cursor);
        return Integer.parseInt(string.substring(DUMMY_CURSOR_PREFIX.length()));
    }


    static public class Base64 {

        private Base64() {
        }

        public static String toBase64(String string) {
            return DatatypeConverter.printBase64Binary(string.getBytes(StandardCharsets.UTF_8));
        }

        public static String fromBase64(String string) {
            return new String(DatatypeConverter.parseBase64Binary(string), Charset.forName("UTF-8"));
        }
    }
}
