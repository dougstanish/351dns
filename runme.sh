javac src/dns351.java
echo "ARCHIVE=\`awk '/^__ARCHIVE_BELOW__/ {print NR + 1; exit 0; }' \$0\`" > 351dns
echo "tail -n+\$ARCHIVE \$0 | tar xzv -C \`pwd\`" >> 351dns
echo "java dns351 \$@" >> 351dns
echo "rm dns351.class" >> 351dns
echo "exit \$?" >> 351dns
echo "__ARCHIVE_BELOW__" >> 351dns
cd src
tar -Ocz dns351.class >> ../351dns
rm dns351.class
cd ..
chmod +x 351dns
