package ru.nvkz.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.nvkz.configuration.R2dbcConfig;
import ru.nvkz.dto.CategoryFiltersResponse;
import ru.nvkz.dto.FilterValue;
import ru.nvkz.dto.ProductFullResponse;
import ru.nvkz.dto.ProductSearchRequest;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class ProductCustomRepositoryImpl implements ProductCustomRepository {
    private final DatabaseClient databaseClient;
    private final R2dbcConfig.MapToJsonConverter mapToJsonConverter;
    private final R2dbcConfig.JsonToMapConverter jsonToMapConverter;

    private record RowData(String key, String value, Long count) {}


    @Override
    public Mono<CategoryFiltersResponse> getFiltersByCategory(Long categoryId) {

        String sql = """
                  SELECT key, value, COUNT(*)
                        FROM products, jsonb_each_text(attributes)
                        WHERE category_id = :catId
                        GROUP BY key, value
                """;


        return databaseClient.sql(sql)
                .bind("catId", categoryId)
                .map(((row, rowMetadata) ->
                        new RowData(
                                row.get("key", String.class),
                                row.get("value", String.class),
                                row.get("count", Long.class)
                        ))
                )
                .all()
                .collectMultimap(
                        RowData::key,
                        rowData -> new FilterValue(rowData.value(), rowData.count()))
                .map(CategoryFiltersResponse::new);
    }

    @Override
    public Flux<ProductFullResponse> findAllByFilter(ProductSearchRequest filter, Integer pageSize, Integer pageNumber) {
        StringBuilder sql = new StringBuilder("""
                    SELECT p.id, p.name, p.price, c.name as category_name, p.attributes
                    FROM products p
                    JOIN categories c ON p.category_id = c.id
                    WHERE 1=1
                """);

        Map<String, Object> params = new HashMap<>();

        if (filter.namePart() != null && !filter.namePart().isBlank()) {
            sql.append(" AND p.name LIKE :name");
            params.put("name", "%" + filter.namePart() + "%");
        }

        if (filter.categoryId() != null) {
            sql.append(" AND p.category_id = :catId");
            params.put("catId", filter.categoryId());
        }

        if (filter.minPrice() != null) {
            sql.append(" AND p.price >= :min");
            params.put("min", filter.minPrice());
        }

        if (filter.maxPrice() != null) {
            sql.append(" AND p.price <= :max");
            params.put("max", filter.maxPrice());
        }

        Map<String, Object> attrMap = filter.getAttrsAsMap();

        if (attrMap != null && !attrMap.isEmpty()) {
            sql.append(" AND p.attributes @> :jsonFilter::jsonb");
            params.put("jsonFilter", mapToJsonConverter.convert(attrMap));
        }

        sql.append(" LIMIT :limit OFFSET :offset");
        params.put("limit", pageSize);
        params.put("offset", pageSize * pageNumber);


        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString());

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            spec = spec.bind(entry.getKey(), entry.getValue());
        }

        return spec.map((row, metadata) -> new ProductFullResponse(
                row.get("id", Long.class),
                row.get("name", String.class),
                row.get("price", BigDecimal.class),
                row.get("category_name", String.class),
                jsonToMapConverter.convert(row.get("attributes", io.r2dbc.postgresql.codec.Json.class))
        )).all();
    }


}
