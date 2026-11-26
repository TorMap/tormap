#!/bin/bash -eu
# Builds the Jazzer fuzz targets of the backend into $OUT, with a launcher script per target (ClusterFuzzLite build step)
cd "$SRC/tormap/backend"
JAVA_HOME="$SRC/jdk" ./gradlew --no-daemon --console=plain installFuzzTargets -PjazzerApi="$JAZZER_API_PATH"

cp build/fuzz/*.jar "$OUT/"
cp -r "$SRC/jdk" "$OUT/jdk"
runtime_classpath=$(cd build/fuzz && printf "\$this_dir/%s:" *.jar)

for source in src/fuzz/kotlin/org/tormap/fuzz/*Fuzzer.kt; do
  target=$(basename "$source" .kt)
  # The comment below makes ClusterFuzzLite detect the script as a fuzz target.
  # The targets do no file access, and Jazzer's path traversal hook fails on strings like new Scanner(String) parses.
  cat > "$OUT/$target" << EOF
#!/bin/bash
# LLVMFuzzerTestOneInput
this_dir=\$(dirname "\$0")
JAVA_HOME="\$this_dir/jdk" LD_LIBRARY_PATH="\$this_dir/jdk/lib/server:\$this_dir" \\
  "\$this_dir/jazzer_driver" --agent_path="\$this_dir/jazzer_agent_deploy.jar" \\
  --cp="${runtime_classpath%:}" --target_class=org.tormap.fuzz.$target \\
  --disabled_hooks=com.code_intelligence.jazzer.sanitizers.FilePathTraversal \\
  --jvm_args="-Xmx2048m:-Djava.awt.headless=true" "\$@"
EOF
  chmod +x "$OUT/$target"
  if [ -d "src/fuzz/seeds/$target" ]; then
    (cd "src/fuzz/seeds/$target" && zip -q -r "$OUT/${target}_seed_corpus.zip" .)
  fi
done
