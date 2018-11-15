package io.github.devwillee.koreametrograph.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

public final class MetroGraph extends AbstractGraph<Station> {

    public MetroGraph() {
        graphType = GraphType.UNDIRECTED;
    }

    private final String[] lineNums = {"A", "B", "E", "G", "I", "I2", "K", "KK", "S", "SU", "U", "UI"};

    public void setSubLine(String fromStationName, String toStationName, String lineNum) {
        for(AbstractGraph<Station>.Edge e : getEdges(fromStationName)) {
            if(e.getToVertex().getStationName().equals(toStationName) && e.getToVertex().getLineNum().equals(lineNum)) {
                e.getToVertex().setMainLine(false);
                break;
            }
        }
    }

    public void removeEdgeSymmetry(String fromStationName, String toStationName, String lineNum) {
        removeEdge(fromStationName, toStationName, lineNum);
        removeEdge(toStationName, fromStationName, lineNum);
    }

    @Override
    public void addVertex(Station... vertices) {
        for(Station vertex : vertices) {

            //이미 vertex가 존재하는가?
            if(edgesByVertices.containsKey(vertex))
                continue;

            // Key와 Value가 완전히 참조가 분리되도록 깊은 복사
            Station vertexCopy = deepCopy(vertex);
            vertexCopy.setIdentifier(Identifier.CURRENT);
            edgesByVertices.put(vertexCopy, new LinkedList<>());
        }
    }

    public LinkedList<AbstractGraph<Station>.Edge> getEdges(String stationName) {
        return super.getEdges(new Station(stationName, "", Identifier.CURRENT));
    }

    // 일반 간선 추가
    @Override
    public void addEdge(Station fromVertex, Station... toVertices) {
        if (!edgesByVertices.containsKey(fromVertex))
            throw new NullPointerException("The fromVertex is not exists.");

        for(Station toVertex : toVertices) {
            if (!edgesByVertices.containsKey(toVertex))
                throw new NullPointerException("The toVertex is not exists.");

            toVertex.setIdentifier(Identifier.NEXT);
            fromVertex.setIdentifier(Identifier.PREVIOUS);


            LinkedList<Edge> edges = edgesByVertices.get(fromVertex);
            Edge newEdge = new Edge(fromVertex, toVertex);
            // 중복 추가 방지 (정점 하나에 이어진 정점이 2개 이상 포함될 수가 없으므로)
            if(edges.contains(newEdge))
                return;

            edges.add(newEdge);

            // 무방향 그래프 대칭 처리
            if (graphType == GraphType.UNDIRECTED)
                addVertexForUndirectedGraph(toVertex, fromVertex);
        }

        // fromVertex만 현재역으로 변환
        for(LinkedList<Edge> vertex : edgesByVertices.values()) {
            for(Edge edge : vertex) {
                edge.fromVertex.setIdentifier(Identifier.CURRENT);
            }
        }
    }

    public void removeEdge(String fromStationName, String toStationName, String lineNum) {
        LinkedList<Edge> edges = edgesByVertices.get(new Station(fromStationName, lineNum, Identifier.CURRENT));

        int result = Integer.MIN_VALUE;

        for(int i=0 ; i<edges.size() ; i++) {
            AbstractGraph<Station>.Edge e = edges.get(i);

            if(e.toVertex.getStationName().equals(toStationName) && e.toVertex.getLineNum().equals(lineNum)) {
                result = i;
                break;
            }
        }

        if(result < 0)
            throw new NullPointerException("Not found");

        edges.remove(result);
    }

    // 무방향 그래프 대칭 처리
    private void addVertexForUndirectedGraph(Station toVertex, Station fromVertex) {

        // 참조 문제로 인한 Bug 해결
        Station toVertexCopy = deepCopy(toVertex);
        Station fromVertexCopy = deepCopy(fromVertex);

        if (!edgesByVertices.containsKey(toVertexCopy))
            edgesByVertices.put(toVertexCopy, new LinkedList<>());

        fromVertex.setIdentifier(Identifier.PREVIOUS);
        toVertex.setIdentifier(Identifier.NEXT);

        LinkedList<Edge> edges = edgesByVertices.get(toVertexCopy);
        edges.add(new Edge(toVertexCopy, fromVertexCopy));
    }

    private boolean checkLineNum(String lineNum) {
        boolean result = false;

        try {
            int lineNum1 = Integer.parseInt(lineNum);
            return 1 <= lineNum1 && lineNum1 <= 9;
        } catch (Exception e) {
            for(String lineNum1 : lineNums) {
                if(lineNum1.equals(lineNum)) {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Depth First Search
     */
    public LinkedList<Station> dfs(boolean isAscending) {
        return dfs(edgesByVertices.firstEntry().getKey(), isAscending);
    }

    public LinkedList<Station> dfs(Station vertex, boolean isAscending) {
        if(!checkLineNum(vertex.getLineNum()))
            throw new IllegalArgumentException("Unavailable lineNum");

        LinkedList<Station> results = new LinkedList<>();

        HashSet<Station> checkVisitSet = new HashSet<>();
        LinkedList<Station> stack = new LinkedList<>();

        //첫 번째 Node 방문
        Station firstVertex = vertex;
        stack.push(firstVertex);
        checkVisitSet.add(firstVertex);

        while(!stack.isEmpty()) {
            Station poppedVertex = stack.pop();
            results.add(poppedVertex);

            sort(edgesByVertices.get(poppedVertex), !isAscending);

            for(Edge edge : edgesByVertices.get(poppedVertex)) {

                Station linkedVertex = edge.getToVertex();
                linkedVertex = edgesByVertices.ceilingKey(linkedVertex); //추가

                if(!checkVisitSet.contains(linkedVertex)) {
                    checkVisitSet.add(linkedVertex);
                    stack.push(linkedVertex);
                }
            }
        }

        return results;
    }

    /**
     * Beneath First Search
     */
    public LinkedList<Station> bfs(Station vertex, boolean isAscending) {
        LinkedList<Station> results = new LinkedList<>();

        HashSet<Station> checkVisitSet = new HashSet<>();

        Queue<Station> queue = new LinkedList<>();

        //첫 번째 Node 방문
        Station firstVertex = getVertex(vertex);
        queue.offer(firstVertex);
        checkVisitSet.add(firstVertex);

        while(!queue.isEmpty()) {
            Station dequeuedVertex = queue.poll();
            results.add(dequeuedVertex);

            sort(edgesByVertices.get(dequeuedVertex), isAscending);

            for(Edge edge : edgesByVertices.get(dequeuedVertex)) {
                Station linkedVertex = edge.getToVertex();
                linkedVertex = edgesByVertices.ceilingKey(linkedVertex); //추가

                if(!checkVisitSet.contains(linkedVertex)) {
                    checkVisitSet.add(linkedVertex);
                    queue.offer(linkedVertex);
                }
            }
        }

        return results;
    }

    /**
     * Beneath First Search
     */
    public LinkedList<Station> bfs(boolean isAscending) {
        return bfs(edgesByVertices.firstEntry().getKey(), isAscending);
    }

    @SuppressWarnings("unchecked")
    private <T> T deepCopy(T obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (T)ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Wrapping. 편하게 쓰기 위한 Helper Method.
     * @param stationName
     * @return
     */
    public List<Station> find(String stationName, String lineNum) {
        try {
            //return getEdges(stationName).stream().filter(edge -> edge.getToVertex().getLineNum().equals(lineNum)).map(Edge::getToVertex).collect(Collectors.toList());

            // 속도에 민감하다면 그냥 for 사용.
            ArrayList<Station> list = new ArrayList<>();
            for(Edge edge : getEdges(stationName)) {
                Station toVertex = edge.getToVertex();

                if(toVertex.getLineNum().equals(lineNum)) {
                    list.add(toVertex);
                }
            }

            return list;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Wrapping. 편하게 쓰기 위한 Helper Method.
     * @param stationName
     * @return
     */
    public List<Station> find(String stationName) {
        try {
            List<Station> list = new ArrayList<>();

            for(Edge e : getEdges(stationName)) {
                list.add(e.getToVertex());
            }

            return list;

            //return getEdges(stationName).stream().map(Edge::getToVertex).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}