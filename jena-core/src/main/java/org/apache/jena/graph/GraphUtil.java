/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jena.graph;

import java.util.ArrayList;
import java.util.Iterator ;
import java.util.List ;
import java.util.Set ;

import org.apache.jena.graph.impl.GraphWithPerform ;
import org.apache.jena.util.IteratorCollection ;
import org.apache.jena.util.iterator.ExtendedIterator ;
import org.apache.jena.util.iterator.WrappedIterator ;

/**
    An ad-hoc collection of useful code for graphs
 */
public class GraphUtil
{
    /**
     * Only static methods here - the class cannot be instantiated.
     */
    private GraphUtil()
    {}

    /** Return an iterator over the unique subjects with predciate p and object o.
     * p and o can be wildcards (Node.ANY)
     * @param g Graph  
     * @param p Predicate - may be Node.ANY
     * @param o Object  - may be Node.ANY
     * @return  ExtendedIterator
     */
    public static ExtendedIterator<Node> listSubjects(Graph g, Node p, Node o) { 
        // Restore a minimal QueryHandler?
        ExtendedIterator<Triple> iter = g.find(Node.ANY, p, o) ;
        Set<Node> nodes = iter.mapWith(t -> t.getSubject()).toSet() ;
        return WrappedIterator.createNoRemove(nodes.iterator()) ;
    }
    
    /** Return an iterator over the unique predicate between s and o.
     * s and o can be wildcards (Node.ANY)
     * @param g Graph  
     * @param s Subject - may be Node.ANY
     * @param o Object  - may be Node.ANY
     * @return  ExtendedIterator
     */
    public static ExtendedIterator<Node> listPredicates(Graph g, Node s, Node o) {
        ExtendedIterator<Triple> iter = g.find(s,Node.ANY, o) ;
        Set<Node> nodes = iter.mapWith(t -> t.getPredicate()).toSet() ;
        return WrappedIterator.createNoRemove(nodes.iterator()) ;
    }
    
    /** Return an iterator over the unique objects with a given subject and object.
     * s and p can be wildcards (Node.ANY)
     * @param g Graph  
     * @param s Subject - may be Node.ANY
     * @param p Predicate  - may be Node.ANY
     * @return  ExtendedIterator
     */
    public static ExtendedIterator<Node> listObjects(Graph g, Node s, Node p) {
        ExtendedIterator<Triple> iter = g.find(s, p, Node.ANY) ;
        Set<Node> nodes = iter.mapWith(t -> t.getObject()).toSet() ;
        return WrappedIterator.createNoRemove(nodes.iterator()) ;
    }
    
    /** Does the graph use the node anywhere as a subject, predciate or object? */
    public static boolean containsNode(Graph graph, Node node) {
        return
            graph.contains(node, Node.ANY, Node.ANY) ||
            graph.contains(Node.ANY, Node.ANY, node) ||
            graph.contains(Node.ANY, node, Node.ANY) ;
    }
    
    /* Control how events are dealt with in bulk */
    private static final boolean OldStyle = true ; 
    
    /**
     * Answer an iterator covering all the triples in the specified graph.
     * 
     * @param g
     *            the graph from which to extract triples
     * @return an iterator over all the graph's triples
     */
    public static ExtendedIterator<Triple> findAll(Graph g) {
        return g.find(Triple.ANY) ;
    }
    
    public static void add(Graph graph, Triple[] triples) {
        if ( OldStyle && graph instanceof GraphWithPerform )
        {
            GraphWithPerform g = (GraphWithPerform)graph ;
            for (Triple t : triples )
                g.performAdd(t) ;
            graph.getEventManager().notifyAddArray(graph, triples) ;
        }
        else
        {
            for (Triple t : triples )
                graph.add(t) ; 
        }
    }
        
    public static void add(Graph graph, List<Triple> triples) {
        addIteratorWorkerDirect(graph, triples.iterator());
        if ( OldStyle && graph instanceof GraphWithPerform )
            graph.getEventManager().notifyAddList(graph, triples) ;
    }
        
    public static void add(Graph graph, Iterator<Triple> it) {
        if ( OldStyle && graph instanceof GraphWithPerform ) {
            // Materialize for the notify.
            List<Triple> s = IteratorCollection.iteratorToList(it) ;
            addIteratorWorkerDirect(graph, s.iterator());
            graph.getEventManager().notifyAddIterator(graph, s) ;
        } 
        else 
            addIteratorWorker(graph, it);
    }
    
    /** Add triples into the destination (arg 1) from the source (arg 2)*/
    public static void addInto(Graph dstGraph, Graph srcGraph ) {
        if ( dstGraph == srcGraph )
            return ;
        dstGraph.getPrefixMapping().setNsPrefixes(srcGraph.getPrefixMapping()) ;
        addIteratorWorker(dstGraph, GraphUtil.findAll( srcGraph ));  
        dstGraph.getEventManager().notifyAddGraph( dstGraph, srcGraph );
    }
    
    private static void addIteratorWorker( Graph graph, Iterator<Triple> it ) { 
        List<Triple> s = IteratorCollection.iteratorToList( it );
        addIteratorWorkerDirect(graph, s.iterator());
    }
    
    private static void addIteratorWorkerDirect( Graph graph, Iterator<Triple> it ) {
        if ( OldStyle && graph instanceof GraphWithPerform ) {
            GraphWithPerform g = (GraphWithPerform)graph;
            while (it.hasNext())
                g.performAdd(it.next());
        } else {
            while (it.hasNext())
                graph.add(it.next());
        }
    }

    public static void delete(Graph graph, Triple[] triples) {
        if ( OldStyle && graph instanceof GraphWithPerform ) {
            GraphWithPerform g = (GraphWithPerform)graph ;
            for ( Triple t : triples )
                g.performDelete(t) ;
            graph.getEventManager().notifyDeleteArray(graph, triples) ;
        } else {
            for ( Triple t : triples )
                graph.delete(t) ;
        }
    }
    
    public static void delete(Graph graph, List<Triple> triples)
    {
        deleteIteratorWorkerDirect(graph, triples.iterator());
        if ( OldStyle && graph instanceof GraphWithPerform )
            graph.getEventManager().notifyDeleteList(graph, triples) ;
    }
    
    public static void delete(Graph graph, Iterator<Triple> it)
    {
        if ( OldStyle && graph instanceof GraphWithPerform ) {
            // Materialize for the notify.
            List<Triple> s = IteratorCollection.iteratorToList(it) ;
            deleteIteratorWorkerDirect(graph, s.iterator());
            graph.getEventManager().notifyDeleteIterator(graph, s) ;
        } else
            deleteIteratorWorker(graph, it);
    }
    
    /** Delete triples in the destination (arg 1) as given in the source (arg 2) */
    public static void deleteFrom(Graph dstGraph, Graph srcGraph) {
        if ( dstGraph == srcGraph ) {
            dstGraph.clear();
            return;
        }
        if ( dstGraph.size() > srcGraph.size() ) {
            // Loop on srcGraph
            deleteIteratorWorker(dstGraph, GraphUtil.findAll(srcGraph)) ;
            dstGraph.getEventManager().notifyDeleteGraph(dstGraph, srcGraph) ;
            return;
        }
        // dstGraph smaller.
        List<Triple> toBeDeleted = new ArrayList<>();
        // Loop on dstGraph
        Iterator<Triple> iter = dstGraph.find(null, null, null);
        for( ; iter.hasNext() ; ) {
           Triple t = iter.next();
           if ( srcGraph.contains(t) )
               toBeDeleted.add(t);
        }
        dstGraph.getEventManager().notifyDeleteGraph(dstGraph, srcGraph) ;
        deleteIteratorWorkerDirect(dstGraph, toBeDeleted.iterator());
    }

    /**
     * Delete the triples supplied by an iterator. This function is "concurrent
     * modification" safe - it internally takes a copy of the iterator.
     */
    private static void deleteIteratorWorker(Graph graph, Iterator<Triple> it) {
        List<Triple> s = IteratorCollection.iteratorToList(it) ;
        deleteIteratorWorkerDirect(graph, s.iterator());
    }

    /**
     * Delete the triples supplied by an iterator. This function is not "concurrent
     * modification" safe; it assumes it can use the iterator while deleting from the
     * graph.
     */
    private static void deleteIteratorWorkerDirect(Graph graph, Iterator<Triple> it) {
        if ( OldStyle && graph instanceof GraphWithPerform ) {
            GraphWithPerform g = (GraphWithPerform)graph ;
            while(it.hasNext())
                g.performDelete(it.next()) ;
        } else {
            while(it.hasNext())
                graph.delete(it.next());
        }
    }

    private static final int sliceSize = 1000 ;
    /** A safe and cautious remove() function that converts the remove to
     *  a number of {@link Graph#delete(Triple)} operations. 
     *  <p>
     *  To avoid any possible ConcurrentModificationExceptions,
     *  it finds batches of triples, deletes them and tries again until
     *  no more triples matching the input can be found. 
     */
    public static void remove(Graph g, Node s, Node p, Node o) {
        // Beware of ConcurrentModificationExceptions.
        // Delete in batches.
        // That way, there is no active iterator when a delete
        // from the indexes happens.

        Triple[] array = new Triple[sliceSize] ;

        while (true) {
            // Convert/cache s,p,o?
            // The Node Cache will catch these so don't worry unduely.
            ExtendedIterator<Triple> iter = g.find(s, p, o) ;

            // Get a slice
            int len = 0 ;
            for ( ; len < sliceSize ; len++ ) {
                if ( !iter.hasNext() )
                    break ;
                array[len] = iter.next() ;
            }

            // Delete them.
            for ( int i = 0 ; i < len ; i++ ) {
                g.delete(array[i]) ;
                array[i] = null ;
            }
            // Finished?
            if ( len < sliceSize )
                break ;
        }
    }
}
