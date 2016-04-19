# gaulois-pipe
A XSLT pipelining solution

gaulois-pipe is a tool that allows to define XSL pipelines, and to apply them on source files.

gaulois-pipe pipelines are made of XSLT, tees and outputs. The source file is transformed by the first XSL, the result is transformed by the second, and so on.
The final step of each branch of a pipe is an output.

gaulois-pipe supports multi-threaded executions. You can define the thread-pool size, the max source file size that may be transformed on a multi-thread engine.

gaulois-pipe supports parameters. Parameters may be assign to whole pipe, sole XSL, or source file.

gaulois-pipe is based on a config file. Schema for such a config file is in
``src/main/resources/fr/efl/chaine/xslt/schemas/saxon-pipe_config.xsd``
There is also an old way to define pipelines via command-line, but this is for backward compatibility only, and should not be used.

Parameters can be defind on command-line, and used in config file, for example to define an output

Here is a very simple config file :
```XML
<config xmlns="http://efl.fr/chaine/saxon-pipe/config"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://efl.fr/chaine/saxon-pipe/config ../../../src/main/resources/fr/efl/chaine/xslt/schemas/saxon-pipe_config.xsd"
    documentCacheSize="2">
    <pipe mutiThreadMaxSourceSize="24349456" nbThreads="4">
        <xslt href="$[xslDir]/parallel.xsl">
            <param name="p-xsl" value="xsl-value"/>
        </xslt>
        <output id="main">
            <folder absolute="${user.dir}/$[destDir]"/>
            <fileName name="${basename}-$[p-file]-result.xml"/>
        </output>
    </pipe>
    <params>
        <param name="p-general" value="GENERAL"/>
    </params>
    <sources>
        <file href="./src/test/resources/source.xml">
            <param name="p-file" value="substitution"/>
        </file>	       
    </sources>
    <params>
        <param name="xslDir" value="./src/test/resources"/>
        <param name="destDir" value="./target/generated-test-files"/>
    </params>
</config>
```
