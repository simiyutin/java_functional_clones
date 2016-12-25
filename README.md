# java_functional_clones

Goal of this project is to learn how to find chunks of java source code which can be functionally refactored and become clones after this refactoring. 
Then we're going to use existing clones find'n'replace algorithms to refactor obtained clones automatically.

IntelliJ IDEA has been chosen as development environment and provider of base refactoring mechanisms.

STEPS:

0. For each file:
1. We find places where builtin functional refactorings can be applied, and apply them.
2. In obtained refactored methods we choose expressions to extract as first-class functions paramaters, and extract them.
3. We launch builtin mechanism for finding and removing clones, and remove them. 

Example of obtained result can be seen here: //todo
