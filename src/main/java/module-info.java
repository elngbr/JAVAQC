module org.redfx.strange {
    requires java.logging;
    requires java.net.http;
    requires org.json;

    exports org.redfx.strange;
    exports org.redfx.strange.algorithm;
    exports org.redfx.strange.gate;
    exports org.redfx.strange.local;
    exports org.redfx.strange.print;
}
