import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class Program {
    public static void main(String[] args) {
        try {
            // *************************************************************************************
            // Datei auswählen
            // *************************************************************************************
            var currentDir = Paths.get("").toAbsolutePath();
            List<Path> files = Files
                    .find(currentDir, 1,
                            (path, attributes) -> path.toString().endsWith(".csv")
                                    || path.toString().endsWith(".txt")
                                    || path.toString().endsWith(".tsv"))
                    .sorted()
                    .collect(Collectors.toList());
            System.out.println(String.format("GEFUNDENE DATEIEN IN %s:", currentDir));
            int i = 0;
            for (var file : files)
                System.out.println(String.format("[%d]: %s", i++, file));

            int filenr = -1;
            var scanner = new Scanner(System.in);
            do {
                try {
                    System.out.print("Waehle die Datei (Abbruch mit CTRL+C): ");
                    filenr = scanner.nextInt();
                } catch (InputMismatchException e) {
                } catch (NoSuchElementException e) {
                }
            } while (filenr < 0 || filenr >= files.size());
            scanner.close();
            var file = files.get(filenr).toString();

            // *************************************************************************************
            // Graphen aufbauen und analysieren
            // *************************************************************************************
            var graph = Graph.fromFile(file);
            System.out.println("ANALYSE DES GRAPHEN:");
            System.out.println("Distanzmatrix des Graphen (leer = unendlich):");
            printDistanceMatrix(graph.getDistanceMatrix());

            System.out.println(String.format("Anzahl der Knoten: %d", graph.nodeCount));
            System.out.println(String.format("Anzahl der Kanten: %d", graph.getEdgeCount()));

            System.out.println("Knotengrade:");
            var degrees = graph.getNodes()
                    .mapToObj(node -> String.format("[%d]: %d", node, graph.getDegree(node)))
                    .collect(Collectors.joining(", "));
            System.out.println(degrees);

            var subgraphs = graph.getSubgraphs();
            System.out.println(String.format("Der Graph hat %d Komponenten:", subgraphs.size()));
            subgraphs.forEach(subgraph -> System.out.println(Arrays.toString(subgraph)));

            System.out.println("Exzentrizitaeten der Knoten:");
            var eccentricities = graph.getNodes()
                    .mapToObj(node -> {
                        var eccentricity = graph.getEccentricity(node);
                        return String.format("[%d]: %s", node,
                                eccentricity == Graph.INF ? "inf" : eccentricity);
                    })
                    .collect(Collectors.joining(", "));
            System.out.println(eccentricities);

            var diameter = graph.getDiameter();
            System.out.println(String.format("Durchmesser des Graphen: %s",
                    diameter == Graph.INF ? "inf" : diameter));
            var radius = graph.getRadius();
            System.out.println(String.format("Radius des Graphen:      %s",
                    radius == Graph.INF ? "inf" : radius));

            System.out.print("Zentrum des Graphen:             ");
            System.out.println(Arrays.toString(graph.getCenter()));
            System.out.print("Artikulationspunkte des Graphen: ");
            System.out.println(Arrays.toString(graph.getArticulations().toArray()));
            System.out.print("Bruecken des Graphen:            ");
            System.out.println(Arrays.deepToString(graph.getEdgeSeparators().toArray()));

        } catch (GraphException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private static void printDistanceMatrix(int[][] matrix) {
        var nodes = matrix.length;
        System.out.print("   ");
        IntStream.range(0, nodes)
                .forEach(i -> System.out.print(String.format("%3s", i)));
        System.out.println();
        for (int row = 0; row < nodes; row++) {
            System.out.print(String.format("%3s", row));
            for (int col = 0; col < nodes; col++) {
                var val = matrix[row][col];
                System.out.print(String.format("%3s", val == Graph.INF ? "" : val));
            }
            System.out.println();
        }
    }
}