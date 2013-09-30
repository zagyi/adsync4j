## ADSync4J - Active Directory Synchronization for Java

[![Build Status](https://travis-ci.org/zagyi/adsync4j.png?branch=develop)](https://travis-ci.org/zagyi/adsync4j)

#### What is ADSync4J?
ADSync4J is a lightweight Java library that greatly simplifies the task of creating and maintaining a replica of objects living in Active Directory.

The library implements a protocol that enables users to download an arbitrary set of data from Active Directory into a local storage *and* to keep it up-to-date with the source by performing periodic incremental synchronization. In short, ADSync4J helps **replicating data from Active Directory**.

#### Why would you want to replicate data from Active Directory?
There can be many use-cases. For example, a typical problem you might face when interacting with Active Directory through LDAP is the poor querying interface that LDAP offers. This becomes an issue if you frequently need to perform queries that go beyond what is feasible using LDAP filters. With ADSync4J it's easy to replicate directory objects you are interested in into a relational or graph database, and leverage the more expressive query language they offer.

ADSync4J can also be used to solve integration problems. If you need to deploy your application into an environment where user accounts come from a diverse set of sources including Active Directory, then ADSync4J will be of great help in keeping the central identity store of the application in sync with Active Directory.

#### How to use this library?
Please refer to the detailed documentation on the [project wiki pages](https://github.com/zagyi/adsync4j/wiki).
