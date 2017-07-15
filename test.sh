    git checkout -b $1
    git add .
    git commit -m $2
    git checkout master
    git pull upstream master
    git checkout $1
    git rebase master
    git push -f origin $1