# jvmcom

Communicate between JVM instances.

## Usage

Show all usages: ```ant help```

Start server: ```ant server```

Start interactive client: ```ant client```

## Todos

  * setup with 2+ Tomcats and a sample webapps which communicate
  * SSL: https://stackoverflow.com/a/53325115
  * authentication of client when connecting to server
  
  
# Meshing

## Participants

Potential participant nodes of the mesh must be well known and will not be added
over the network.

Every participant must be defined in the mesh.properties file which should be
accessible to all nodes. Of course a reload of this file can be triggered over
the network.

Participants have a hostname, port and id.

## Joining

A newly started node will ping all other defined participant nodes.
Should it find no other nodes, it will be its own mesh.

Should it find other nodes, it will send a "join <ID>" to all other
active nodes. The other nodes will then add the node to their list of
active nodes.

## Leaving

### Orderly leaving

Leaving node sends a "leave <ID" to all other nodes, which can then
remove the id from their list of active nodes.

### Unexpected leaving

If a node crashes the other nodes will of course not be notified automatically.
Should one node come across a serve no longer being available it will send
an "lost <ID>" to all active nodes for them be updated about the node loss.

A health service could also be built, to regularly ping all nodes and report
any unexpected joins or losses.

## Meshing service

The meshing service is the main service which must be supported by every
node. It runs on the configured port of the node. Other services may run
on other ports of the node.

All nodes must provide these services on request:

- ping: reply with node id
- join ID: add ID to list of active nodes
- leave ID: remove ID from list of active nodes
- lost ID: same as leave
- reload: reload the mesh configuration file
- stop: shut down node



















  
  
  
