# Contributing to Payara

As with many open source projects Payara is hosted on Github, allowing anyone to contribute code and help with its development. To make sure that development is coordinated and that changes are easily tracked, we have a series of steps that should be followed in order to get your code merged.

## Legal Bits
Payara is an open source project, with the code owned by Payara Foundation a United Kingdom based not for profit company limited by guarantee. As Payara Foundation are the custodians of the code, we have specific legal requirements concerning how we distribute code contributed to the project. Before any code contributed by our community is pulled into our repository we must have a signed Contributor License Agreement from any contributor. This can be downloaded from the main repository at [https://github.com/payara/Payara/blob/master/PayaraCLA.pdf](https://github.com/payara/Payara/blob/master/PayaraCLA.pdf) and should be signed, scanned, and forwarded to [cla@payara.org](mailto:cla@payara.org). As compensation for wading through the legalese, all contributors who send in a signed Contributor License Agreement receive a Payara goodie bag.

As we must also comply with the upstream Oracle Common Development and Distribution license the following line should be added to any changed file:

```
Portions Copyright [2017] Payara Foundation
```

## Getting Payara
You will need to create a personal Github account and fork the repository.
Once you have your own up-to-date version of payara, you can now download it to your computer.

Install git on your local environment and use the below command to download your remote copy of payara:

```
git clone https://github.com/<YourUsername>/Payara
```

Git works using "repositories" - stores of data. By default, you have your remote repository on Github, as well as your local repository on your computer. To ensure that future versions of payara incorporate everyones changes, in addition to the current branch there is an upstream branch, where merged changes can be stationed before being added to the project. Adding your own remote repository as the default ("origin") and the upstream payara repository will ensure that you are always able to synchronise yourself with the project as it goes forward. Run the following two commands:

```
git remote add upstream https://github.com/payara/Payara
```

```
git remote add origin https://github.com/<YourUsername>/Payara
```

You are now free to start working on Payara issues, adding new features, or tinkering with the codebase.

## Building Payara
Payara uses maven to build the server, you can use either JDK 7 or JDK 8 to build Payara Server, we distribute Payara built with JDK7 for backwards compatibility with GlassFish.
To build Payara from the root of the cloned source code tree execute;
```
mvn -DskipTests clean package
```
When finished the Payara distribution zip file will be available in the directory;
```
appserver\distributions\payara\target\payara.zip
```


## Updating your fork
As Payara is under continuous development, our upstream branch is regularly updated with dev and community commits. It is worth synchronising your repository with the upstream repo you added previously.

To get the latest updates from upstream and merge them into your local repo, enter the following command:

```
git fetch upstream
```

Then ensure that you are on your local master branch (as opposed to any issue branches you may have):

```
git checkout master
```

Finally, pull in the changes from upstream to your master and update your remote repository:

```
git pull upstream master
```

```
git push origin master
```

## Working on an issue
To start working on an issue, create a new branch on your github repo with the following command:

```
git checkout -b <BranchName>
```

*Please don't prepend PAYARA to your branch unless you have been given a JIRA ticket to work on*

If you are working on a GitHub issue a good name for your branch could be issue-### where ### is the number.

Start working on your project within your IDE and make any changes you wish.

## Debugging Payara

Once you have built Payara Server the full distribution will be available within your local repository under the path

```
<YourLocalRepo>/appserver/distributions/payara/target/stage
```

In order to debug Payara, first build the server with your changes. Run it in debug mode by using the following command:

```
./asadmin start-domain --verbose --debug
```

From within your IDE you can then attach a debugger to the default port of 9009.

## Pushing issues to Github

When you are finished working on your issue, add the files to your git with a comment describing the addressed issue via JIRA and/or the Github issue number:

```
git add  . [or specify specific files
```

```
git commit -m "fixes #<GithubNumber>"
```

Before you merge the branch, ensure that you have updated your master to match the upstream payara. This can be accomplished by using the following:

First, switch to the master branch:

```
git checkout master
```

Then synchronise your branch with the changes from master:

```
git pull upstream master
```

Flip back to your own branch, with your changes:

```
git checkout <YourBranchName>
```

Merge said changes with the master branch by rebasing your code (effectivaly a neater marge for private repos):

```
git rebase master
```

Finally, push the changes from your branch to a new branch on the main repo (origin), with the same name (so as to preserve the issue numbers and history):

```
git push origin <YourBranchName>:<YourBranchName>
```

## Feature requests and issues

A large portion of our work is prompted by the actions of the community. If you have an issue which you have found with Payara, or a feature which you would like to be implemented we welcome the raising of github issues.

## Reporting bugs

If you find a bug within Payara, please post it as a github issue. Github is our main repository for community found issues with Payara and our support team frequently monitor it for new issues. As with submitting issues, a concise title which clearly explains the issue combined with a comment explaining what the found issue is and either how it arose and a stacktrace of the issue, or a test case which is able to reproduce the issue will help us deliver a patch.

## Responses

We continually check the github posted issues for bugs, feature requests, and assorted issues. If you have posted an issue, chances are it has been read by a member of staff. Requests for further information and labels are often posted in order to make it easier for the dev team to see issues. However if your issue has not received a comment or label, don't take this as it having not been read or acted upon!
