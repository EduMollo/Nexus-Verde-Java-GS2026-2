package br.com.fiap.nexusverde.iot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Stream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class Procedimentos {
    public record Fonte(String codigo, String titulo, String trecho) {}
    private record Resultado(Fonte fonte, double score) {}
    private final List<Fonte> fontes;
    private final Map<String,Double> idf = new HashMap<>();
    private final Map<Fonte,Map<String,Double>> vetores = new LinkedHashMap<>();

    public Procedimentos() throws IOException {
        var corpus = new ClassPathResource("iot/procedimentos.tsv").getContentAsString(StandardCharsets.UTF_8);
        fontes = corpus.lines().filter(l -> !l.isBlank()).map(l -> l.split("\\t", 3))
                .map(c -> new Fonte(c[0], c[1], c[2])).toList();
        Map<String,Integer> frequencia = new HashMap<>();
        for (Fonte f : fontes) {
            palavras(f.titulo()+" "+f.trecho()).distinct().forEach(t -> frequencia.merge(t,1,Integer::sum));
        }
        frequencia.forEach((t,n) -> idf.put(t, 1 + Math.log((1.0+fontes.size())/(1+n))));
        for (Fonte f : fontes) vetores.put(f, vetor(f.titulo()+" "+f.trecho()));
    }

    private static Stream<String> palavras(String texto) {
        var normal = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return Arrays.stream(normal.split("[^a-z0-9]+")).filter(t -> t.length()>2);
    }

    private Map<String,Double> vetor(String texto) {
        Map<String,Double> valores = new HashMap<>();
        palavras(texto).filter(idf::containsKey).forEach(t -> valores.merge(t,idf.get(t),Double::sum));
        double norma = Math.sqrt(valores.values().stream().mapToDouble(v -> v*v).sum());
        valores.replaceAll((t,v) -> v/norma);
        return valores;
    }

    public List<Fonte> buscar(String consulta) {
        var q = vetor(consulta);
        return fontes.stream().map(f -> new Resultado(f,q.entrySet().stream()
                .mapToDouble(e -> e.getValue()*vetores.get(f).getOrDefault(e.getKey(),0.0)).sum()))
                .filter(r -> r.score()>0).sorted(Comparator.comparingDouble(Resultado::score).reversed())
                .limit(3).map(Resultado::fonte).toList();
    }
}
