package com.github.samblake.yamlrewrite;

import com.github.samblake.yamlrewrite.io.RealFileSystem;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * Mojo for transforming YAML files using a transformation specification.
 *
 * Usage in pom.xml:
 * <plugin>
 *   <groupId>com.github.samblake.yamlrewrite</groupId>
 *   <artifactId>yamlrewrite-maven-plugin</artifactId>
 *   <executions>
 *     <execution>
 *       <goals>
 *         <goal>rewrite</goal>
 *       </goals>
 *       <configuration>
 *         <sourceFile>src/main/resources/config.yaml</sourceFile>
 *         <transformationFile>src/main/resources/transformations.yaml</transformationFile>
 *         <outputFile>target/config-transformed.yaml</outputFile>
 *       </configuration>
 *     </execution>
 *   </executions>
 * </plugin>
 */
@Mojo( name = "rewrite", defaultPhase = LifecyclePhase.PROCESS_RESOURCES )
public class YamlRewriteMojo extends AbstractMojo {

    /**
     * The source YAML file to transform.
     */
    @Parameter( property = "sourceFile", required = true )
    private File sourceFile;

    /**
     * The YAML file containing transformation specifications.
     */
    @Parameter( property = "transformationFile", required = true )
    private File transformationFile;

    /**
     * The output file for the transformed YAML.
     * If not specified, the source file will be modified in place.
     */
    @Parameter( property = "outputFile", required = false )
    private File outputFile;

    public void execute() throws MojoExecutionException {
        try {
            getLog().info( "Transforming YAML file: " + sourceFile.getAbsolutePath() );
            getLog().info( "Using transformation file: " + transformationFile.getAbsolutePath() );

            YamlTransformer transformer = new YamlTransformer( new RealFileSystem() );
            transformer.transform(
                sourceFile.getAbsolutePath(),
                transformationFile.getAbsolutePath(),
                outputFile != null ? outputFile.getAbsolutePath() : null
            );

            if ( outputFile != null ) {
                getLog().info( "Transformed YAML written to: " + outputFile.getAbsolutePath() );
            } else {
                getLog().info( "Source file modified in place" );
            }
        } catch ( IllegalArgumentException e ) {
            throw new MojoExecutionException( "Invalid transformation specification: " + e.getMessage(), e );
        } catch ( Exception e ) {
            throw new MojoExecutionException( "Error transforming YAML file", e );
        }
    }
}
