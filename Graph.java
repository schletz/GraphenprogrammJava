import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.regex.MatchResult;

class Graph {
    private final int[][] adjacency;
    private final int[][] distanceMatrix;
    public final int nodeCount; // Die Anzahl der Knoten des eingelesenen Graphen.
    public static final int INF = Integer.MAX_VALUE;

    /**
     * Konstruktor. Initialisiert die Adjazenzmatrix und berechnet die Distanzmatrix.
     */
    private Graph(int[][] adjacency) {
        this.adjacency = adjacency;
        nodeCount = adjacency.length;
        distanceMatrix = new int[nodeCount][nodeCount];
        calcDistanceMatrix();
    }

    public int getEdgeCount() {
        int count = 0;
        // Der Graph ist ungerichtet, wir zählen daher nur den unteren Bereich der
        // Adjazenzmatrix.
        for (int row = 0; row < nodeCount; row++)
            for (int col = 0; col <= row; col++)
                if (adjacency[row][col] > 0)
                    count++;
        return count;
    }

    /**
     * Der kürzeste Abstand zwischen 2 Knoten. Wird im Konstruktor vorberechnet. Dies ist die wichtigste Datenstruktur, auf
     * deren Basis alle Metriken berechnet werden. Es wird int? verwendet, für unendliche Distanzen wird null gespeichert.
     */
    public int[][] getDistanceMatrix() {
        return distanceMatrix;
    }

    /**
     * Liefert eine Zahlenliste aller Knoten, von 0 beginnend (0, 1, 2, ..., nodeCount-1) Das brauchen wir, um einfach durch
     * die Knoten mit einer Schleife iterieren zu können.
     */
    public IntStream getNodes() {
        return IntStream.range(0, nodeCount);
    }

    /**
     * Ein Graph ist zusammenhängend, wenn ich von einem Knoten alle anderen erreichen kann. --> Die Distanz ist immer
     * kleiner unendlich, also ungleich null. Wir nehmen den ersten Knoten ohne Beschränkung der Allgemeinheit, denn in
     * einem zusammenhängenden Graphen ist auch jeder Knoten mit dem ersten Knoten verbunden.
     */
    public Boolean isConnected() {
        return !getNodes().anyMatch(node -> distanceMatrix[0][node] == INF);
    }

    /**
     * Der Durchmesser ist unendlich, wenn der Graph nicht zusammenhängend ist. Ansonsten nehmen wir die maximale
     * Exzentrizität aller Knoten im Graphen.
     */
    public int getDiameter() {
        return !isConnected() ? INF
                : getNodes().map(node -> getEccentricity(node)).max().orElse(INF);
    }

    /**
     * Der Radius ist unendlich, wenn der Graph nicht zusammenhängend ist. Ansonsten nehmen wir die minimale Exzentrizität
     * aller Knoten im Graphen.
     */
    public int getRadius() {
        return !isConnected() ? INF
                : getNodes().map(node -> getEccentricity(node)).min().orElse(INF);
    }

    /**
     * Das Zentrum enthält alle Knoten, dessen Exzentrizität gleich dem Radius ist.
     * http://www.informatik.uni-trier.de/~naeher/Professur/PROJECTS/vogt/Thema.html
     */
    public int[] getCenter() {
        var radius = getRadius();
        if (radius == INF)
            return new int[0];
        return getNodes().filter(node -> getEccentricity(node) == radius).toArray();
    }

    private void ensureValidNode(int node) throws GraphException {
        if (node < 0 || node >= nodeCount)
            throw new GraphException(String.format("Invalid Node %d", node));
    }

    /**
     * Liest eine CSV Datei mit der Adjazenzmatrix ein. Sie hat den Aufbau 1;0;1;0;0... (0 = nicht verbunden, 1 = verbunden)
     * Das Trennzeichen ist beliebig, es werden alle Zahlen in der Zeile gesucht.
     */
    public static Graph fromFile(String filename) throws IOException, GraphException {
        var rowSplitRegex = Pattern.compile("\\d+");
        // Leere Zeilen werden ignoriert. In Datenzeilen werden alle Zahlen gelesen.
        // Somit funktioniert das Lesen mit jedem Trennzeichen.
        List<List<Integer>> lines = Files.lines(Paths.get(filename), StandardCharsets.UTF_8)
                .filter(rowSplitRegex.asPredicate())
                .map(line -> rowSplitRegex.matcher(line).results().map(MatchResult::group)
                        .map(Integer::parseInt).collect(Collectors.toList()))
                .collect(Collectors.toList());
        int nodeCount = lines.size();
        var adjacency = new int[nodeCount][nodeCount];
        int row = 0;
        for (var line : lines) {
            if (line.size() != nodeCount)
                throw new GraphException(
                        String.format("Zeile %d hat eine ungültige Anzahl an Werten. Erwartet: %d.",
                                row + 1, nodeCount));
            int col = 0;
            for (var value : line)
                adjacency[row][col++] = value;
            row++;
        }
        return new Graph(adjacency);
    }

    /**
     * Berechnet die Distanzmatrix für jeden Knoten im Graphen.
     * https://en.wikipedia.org/wiki/Floyd%E2%80%93Warshall_algorithm
     */
    private void calcDistanceMatrix() {
        // Zuerst wird die Distanzmatrix initialisiert.
        // Jeder Knoten hat zu sich selbst den Abstand 0.
        // Ist ein Knoten direkt mit einem anderen Knoten verbunden, so nehmen wir den
        // Wert aus der Adjazenzmatrix.
        for (int row = 0; row < nodeCount; row++)
            for (int col = 0; col < nodeCount; col++) {
                if (row == col) {
                    distanceMatrix[row][col] = 0;
                    continue;
                }
                var weight = adjacency[row][col];
                // Damit wir nicht 0 für nicht verbundene Knoten als Distanzwert schreiben,
                // prüfen wir auf > 0.
                if (weight > 0) {
                    distanceMatrix[row][col] = weight;
                    continue;
                }
                distanceMatrix[row][col] = INF;
            }

        // Idee: Wir halten einen Knoten fest (k).
        // Dann nehmen wir ein Knotenpaar aus dem Graphen (i und j).
        // Wir gehen von i über k nach j (i -> k -> j) und messen den Abstand. Ist der
        // neue Abstand kleiner als der gespeicherte, schreiben wir ihn in die
        // Distanzmatrix. So finden wir den kleinsten Abstand zwischen 2 Knoten. Der k Knoten ist
        // notwendig, da die Knoten ja nicht direkt verbunden sind. Diese haben wir schon im vorigen Schritt
        // initialisiert.
        for (int k = 0; k < nodeCount; k++)
            for (int i = 0; i < nodeCount; i++)
                for (int j = 0; j < nodeCount; j++) {
                    if (distanceMatrix[i][k] == INF)
                        continue;
                    if (distanceMatrix[k][j] == INF)
                        continue;
                    var dist = distanceMatrix[i][k] + distanceMatrix[k][j];
                    if (distanceMatrix[i][j] > dist)
                        distanceMatrix[i][j] = dist;
                }
    }

    /**
     * Gibt den Grad eines Knotens zurück.
     */
    public int getDegree(int node) {
        return (int) getNodes().filter(n -> adjacency[node][n] > 0).count();
    }

    /**
     * Liefert eine Liste aller Knoten, die vom angegebenen Knoten aus erreicht werden können. Erreicht bedeutet, dass auch
     * mehrere Knoten dazwischen liegen können, deswegen verwenden wir die Distanzmatrix und nicht die Adjazenzmatrix.
     */
    private IntStream getReachableNodes(int startNode) {
        return getNodes().filter(node -> distanceMatrix[startNode][node] != INF);
    }

    /**
     * Liefert alle Teilgraphen als Liste von Knotenarrays. Dabei gehen wir vom ersten Knoten im Graphen aus und prüfen über
     * die Distanzmatrix, welche Knoten erreichbar (auch über mehrere Stufen) sind. Diese erreichbaren Knoten werden dann
     * aus der Liste der nicht besuchten Knoten entfernt, und wir prüfen erneut mit dem ersten nicht besuchten Knoten die
     * Distanzmatrix.
     */
    public List<int[]> getSubgraphs() {
        var result = new ArrayList<int[]>();
        // Eine Zahlenfolge 0, ..., n-1 generieren, die alle vorhandenen Knoten
        // auflistet.
        List<Integer> unvisited = new ArrayList<Integer>(getNodes().boxed().toList());
        while (unvisited.size() != 0) {
            // Beim ersten noch nicht besuchten Knoten starten.
            var startNode = unvisited.get(0);
            // Wohin gibt es Wege?
            var reachableNodes = getReachableNodes(startNode);
            // Alle erreichbaren Knoten aus der Liste der noch nicht besuchten Knoten
            // entfernen.
            unvisited.removeAll(reachableNodes.boxed().toList());
            result.add(getReachableNodes(startNode).toArray());
        }
        return result;
    }

    /**
     * Löscht einen Knoten aus der Adjazenzmatrix. Der neue Graph wird zurückgegeben. d. h. der bestehende Graph wird nicht
     * verändert. Der neue Graph hat natürlich 1 Knoten weniger. Das ist für die Ermittlung der Artikulationspunkte wichtig.
     */
    public Graph removeNode(int node) throws GraphException {
        ensureValidNode(node);
        var adjacency = new int[nodeCount - 1][nodeCount - 1];

        // Die Adjazenzmatrix der verbleibenden Knoten wird kopiert.
        for (int row = 0, destRow = 0; row < nodeCount; row++) {
            if (row == node)
                continue;
            for (int col = 0, destCol = 0; col < nodeCount; col++) {
                if (col == node)
                    continue;
                adjacency[destRow][destCol++] = this.adjacency[row][col];
            }
            destRow++;
        }
        return new Graph(adjacency);
    }

    /**
     * Löscht eine Kante aus dem Graphen und gibt den neuen Graphen zurück. Der aktuelle Graph wird nicht verändert.
     */
    public Graph removeEdge(int node1, int node2) throws GraphException {
        ensureValidNode(node1);
        ensureValidNode(node2);

        var adjacency = new int[nodeCount][nodeCount];
        for (int row = 0; row < nodeCount; row++)
            for (int col = 0; col < nodeCount; col++)
                adjacency[row][col] = this.adjacency[row][col];

        adjacency[node1][node2] = 0;
        adjacency[node2][node1] = 0;
        return new Graph(adjacency);
    }

    /**
     * Liefert eine Liste der Artikulationspunkte. Dabei entfernen wir den zu prüfenden Knoten und prüfen, ob der Graph in
     * mehrere Teilgraphen als vorher zerfällt. Siehe https://mathworld.wolfram.com/ArticulationVertex.html
     */
    public IntStream getArticulations() {
        var subgraphCount = getSubgraphs().size();
        return getNodes().filter(node -> {
            try {
                return removeNode(node).getSubgraphs().size() > subgraphCount;
            } catch (GraphException e) {
                return false;
            }
        });
    }

    /**
     * Die Exzentrizität ist die Distanz zum am Weitesten entfernten Knoten. Da wir schon die Distanzmatrix haben, ist die
     * Berechnung einfach. In einem nicht zusammenhängenden Graphen ist die Exzentrizität von jedem Knoten unendlich.
     * https://mathworld.wolfram.com/GraphEccentricity.html
     */
    public int getEccentricity(int startNode) {
        if (!isConnected()) {
            return INF;
        }
        return getNodes().map(node -> distanceMatrix[startNode][node]).max().orElse(INF);
    }

    /**
     * Liefert eine Liste von Kanten, die - wenn man sie entfernt - den Graphen in mehrere Teilgraphen aufteilen würden. Das
     * sind dann Brücken. Da der Graph ungerichtet ist, müssen wir nur den Teil unter der Diagonale der Adjazenzmatrix
     * analysieren.
     */
    public List<int[]> getEdgeSeparators() {
        var result = new ArrayList<int[]>();
        var subgraphCount = getSubgraphs().size();
        for (int node1 = 0; node1 < nodeCount; node1++)
            for (int node2 = 0; node2 <= node1; node2++)
                if (adjacency[node1][node2] > 0) {
                    try {
                        var newGraph = removeEdge(node1, node2);
                        if (newGraph.getSubgraphs().size() > subgraphCount)
                            result.add(new int[] { node2, node1 });
                    } catch (GraphException e) {
                    }
                }
        return result;
    }
}