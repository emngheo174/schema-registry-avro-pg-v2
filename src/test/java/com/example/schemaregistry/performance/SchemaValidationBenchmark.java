package com.example.schemaregistry.performance;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;

import java.util.concurrent.TimeUnit;

public class SchemaValidationBenchmark {

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void validateSchema() {
        String schemaText = "{\"type\": \"record\", \"name\": \"Test\", \"fields\": [{\"name\": \"f1\", \"type\": \"string\"}]}";
        try {
            new org.apache.avro.Schema.Parser().parse(schemaText);
        } catch (Exception e) {
            // ignore
        }
    }
}