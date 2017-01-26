# gaulois-pipe

gaulois-pipe is a xml processing tool, designed to process XML files through XSLT pipelines, in environments where time-consuming is an important constraint.

It provides a language to describe the pipeline, to look for sources, to manage parameters, and to define outputs.

It relies on [Saxon](http://www.saxonica.com) HE, but you can change it for Saxon PE or EE, and use gaulois-pipe with your Saxon license.

## Links with XProc

gaulois-pipe is **not** a replacement for XProc : it only provides a p:xslt equivalent step, but it uses Saxon API to avoid serialization between steps. It is higly multi-threaded, with some limitating mecanisms, to avoid process many huge files together.

To keep XProc compatibility, gaulois-pipe provides a XSLT to translate a gaulois-pipe pipeline definition into a XProc pipeline.

## Issues

In case of defect or question, please report issues to [github](https://github.com/cmarchand/gaulois-pipe/issues/).
