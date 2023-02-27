# jvmcom

Communicate between JVM instances.

# Closed

I'm closing this project, because I found that the technology
used here is too low level for my use case.

I'm exploring redis as a communication platform between jvm now,
using lettuce as a java client.

The new project is called rcache.

The code here is not stable enough for production. It can be used
to gain at least some introductory knowledge into netty.

Netty is a powerhouse for java networking. It is also very complex
and low level. Of course it is one abstraction and optimization
above raw java io and nio, but still it is low.

Netty is high performance netowrking if you have to develop a
large client-server or cluster architecture. It has all the
http and ssl built in already. It is very powerful.

It is like an assemly language, when I needed C or Java.

`* * *`

# History

- early versions < 0.1.9 ware based on pure java.io (git: 4ab1e5e51419e49509bf8099fd49bb58103b5eed)
- later versions from 0.1.9 were pure netty based

