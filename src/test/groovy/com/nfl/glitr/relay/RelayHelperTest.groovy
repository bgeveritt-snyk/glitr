package com.nfl.glitr.relay

import com.nfl.glitr.GlitrBuilder
import com.nfl.glitr.data.query.QueryType
import com.nfl.glitr.util.SerializationUtil
import spock.lang.Specification

class RelayHelperTest extends Specification {

    def glitr = GlitrBuilder.newGlitr()
            .withRelay()
            .withObjectMapper(SerializationUtil.objectMapper)
            .withQueryRoot(new QueryType()).build()
    def typeRegistry = glitr.typeRegistry
    def relayHelper = glitr.relayHelper


    @SuppressWarnings("GroovyPointlessBoolean")
    def "test build connection pagination"() {
        expect:
        testPaging(toSkip, totalCount, hasNextPage, hasPrevPage, resultSize)

        where:
        toSkip  ||  totalCount      ||  hasNextPage     ||  hasPrevPage ||  resultSize
        0       ||  12              ||  true            ||  false       ||  10
        1       ||  12              ||  false           ||  true        ||  10
        2       ||  12              ||  false           ||  true        ||  10
        7       ||  12              ||  false           ||  true        ||  5
        69      ||  71              ||  false           ||  true        ||  2
        70      ||  71              ||  false           ||  true        ||  1
        1       ||  1               ||  false           ||  true        ||  0
        50      ||  50              ||  false           ||  true        ||  0
        11      ||  12              ||  false           ||  true        ||  1
        0       ||  0               ||  false           ||  false       ||  0
        0       ||  1               ||  false           ||  false       ||  1
        1       ||  1               ||  false           ||  true        ||  0
    }

    public void testPaging(def toSkip, def totalCount, def hasNext, def hasPrev, def resultSize) {
        def items = []
        for (def i = 0; i < 10; i++) {
            if ((i + toSkip) < totalCount) {
                items.add(i + toSkip)
            }
        }

        def connection = RelayHelper.buildConnection(items, toSkip, totalCount)
        assert (connection.pageInfo.hasNextPage == hasNext)
        assert (connection.pageInfo.hasPreviousPage == hasPrev)
        assert (connection.edges.size() == resultSize)

        def pageInfoWithTotal = (PageInfoWithTotal) connection.pageInfo
        assert pageInfoWithTotal.total == totalCount

        if (resultSize > 0) {
            assert (connection.pageInfo.startCursor?.value == RelayHelper.createCursor(toSkip))
            assert (connection.pageInfo.endCursor?.value == RelayHelper.createCursor(toSkip + resultSize - 1))

            if (toSkip - resultSize > 0) {
                assert (pageInfoWithTotal.previousPageStartCursor?.value == RelayHelper.createCursor(toSkip - resultSize))
            } else {
                assert (pageInfoWithTotal.previousPageStartCursor?.value == RelayHelper.createCursor(0))
            }
        }

        connection.edges.forEach({
            assert (it.cursor.value == RelayHelper.createCursor(it.node))
        })
    }
}
