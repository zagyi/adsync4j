## ADSync4J - Active Directory Synchronization for Java

ADSync4J is a lightweight Java library that can greatly simplify the task of synchronizing contents from Active Directory to your database, and keeping that content in sync with incremental updates.

### 1. Quick intro
#### What can this library do for you?

The core functionality is basically made up by two operations:

1. Retrieve entries from Active Directory.
2. Retrieve changes made to these entries in incremental updates.

Both of these operations use the LDAP protocol to talk to the Active Directory server.

#### What won't id do for you?

1. The synchronization is one-way only, it does not issue any updates to Active Directory.
2. It cannot read data from Active Directory that is not available through LDAP. In particular, it won't synchronize **user passwords**.
3. It won't persist the data retrieved. Storing the content is out of scope for this library. 

#### How does it work?

You need to give a few parameters to specify **where** you want to synchronize **from**:

1. The Active Directory server's address.
2. User credentials to authenticate with.
3. The distinguished name of the directory's root entry. (e.g. `DC=example,DC=com`)

And a few others to specify **what** you want to synchronize:

1. Distinguished name of the base entry for synchronization (e.g. `CN=Users,DC=example,DC=com`). 
2. LDAP filter defining the entries you are interested in (the scope of synchronization).
3. List of attributes you want to retrieve.

After having all this information, the first thing to do is a full synchronization. Once the initial set of entries are stored in database, you can poll for changes with a frequency suitable for your application. Incremental synchronization will report any changes that has been made in Active Directory to the entries within the defined scope (including updates, inserts and deletes).

## 2. Detailed user guide


