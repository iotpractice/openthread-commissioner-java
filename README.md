# Thread Commissioner Implementation in Java

This project refers android commissioner implementation from [OpenThread](https://github.com/openthread/ot-commissioner/tree/main/android). Replicate similar behaviour 
in plain java application. It builds the native java library from [OpenThread Commissioner Repository](https://github.com/openthread/ot-commissioner) and uses it in Java Application.

# Pre-Built Libraries
Here are few pre-built libraries  
1. [libotcommissioner.jar](pre-built/libotcommissioner.jar) - Cross Platform Java JNI Wrapper
2. [libcommissioner-java-mac-amd_64.jnilib](pre-built/libcommissioner-java-mac-amd_64.jnilib) - For Mac OS with Apple Chips.   
2. [libcommissioner-java-mac-x86_64.jnilib](pre-built/libcommissioner-java-mac-x86_64.jnilib) - For Mac OS with Intel Chips

Rename the respective library to `libcommissioner-java.jnilib` and place it in the same directory as your java application. Embed `libotcommissioner.jar` in your application jar. 

## Build Native Commissioner
Follow the below steps to build Native Commissioner

- Make sure we set JAVA_HOME Environment Variable so that it can find JNI.

    ```bash 
    ➜  $ echo export "JAVA_HOME=\$(/usr/libexec/java_home)" >> ~/.zshrc
    ➜  $ echo $JAVA_HOME
    /Users/thw8316/Library/Java/JavaVirtualMachines/openjdk-23.0.1/Contents/Home  
    ➜  $  
    ```

- Run [build-libs.sh](build-libs.sh) 

  ```bash 
    ➜  $ ./build-libs.sh
  ```
This should generate following jars under [libs](libs)
- `libotcommissioner.jar`
- `libcommissioner-java.jnilib` or `libcommissioner-java.so` depending on the platform. 

## Build Java Commissioner

- Build Project and run it.
```bash
➜ openthread-commissioner-java $ mvn clean package
```
It will package dependent jars in the generated jar and place native libs next to it`openthread-commissioner-java-1.0-SNAPSHOT-jar-with-dependencies.jar`

## Run the Java Commissioner

```bash
➜  openthread-commissioner-java $ cd target  
➜  target $  java -jar openthread-commissioner-java-1.0-SNAPSHOT-jar-with-dependencies.jar 

2025-03-09 21:35:08 INFO  c.thread.commissioner.OTBRDiscoverer - Discovering Border Router at _meshcop._udp.local.
2025-03-09 21:35:08 INFO  com.thread.commissioner.Runner - Discovering Border Router...1
2025-03-09 21:35:09 INFO  com.thread.commissioner.Runner - Discovering Border Router...2
2025-03-09 21:35:10 INFO  com.thread.commissioner.Runner - Discovering Border Router...3
2025-03-09 21:35:10 INFO  c.thread.commissioner.OTBRDiscoverer - Service resolved: 172.20.10.9:49154 OpenThreadDemo 1111111122222222
>>> Enter PSKc (enter blank if want to compute):445f2b5ca6f2a93a55ce570a70efeecb
Commands:
1. Check State
2. Enable All Joiners
3. Exit
Enter command number: 2025-03-09 21:35:19 INFO  com.thread.commissioner.Runner - Commissioner connected successfully!
1
2025-03-09 21:35:23 INFO  com.thread.commissioner.Runner - State:kActive

```

If Border router is not discovered, we can manually provide the IP and Port of the Border Router. If PSKc is not provided, 
it will ask network name, ext-pan-id and passphrase to and generate pskc. 

## Join from a Device

Try joining from thread end device. Build and flash this poc firmware.
Any device can discover the network and join with "J01NME" key. On joining attempt

We would see this in commissioner


```bash
Enter command number: 2025-03-09 22:10:41 INFO  c.t.commissioner.ThreadCommissioner - enableAllJoiners - steeringData=ffffffffffffffffffffffffffffffff A joiner (ID=af5570f5a1810b7a)
2025-03-09 22:10:41 INFO  com.thread.commissioner.Runner - All Joiners are accepted at PSKD:J01NME
2025-03-09 22:11:12 INFO  c.t.commissioner.ThreadCommissioner - A joiner (ID=ca666d7873988c66) is requesting commissioning
2025-03-09 22:11:12 INFO  c.t.commissioner.ThreadCommissioner - A joiner (ID=ca666d7873988c66) is connected with OK
2025-03-09 22:11:12 INFO  c.t.commissioner.ThreadCommissioner - A joiner (ID=ca666d7873988c66) is finalizing

```

This concludes implementing the commissioner in java   

**Note :- This is a proof of concept, and do not recommend directly using the same in production, refer this a proof of concept for Java implementation** 

