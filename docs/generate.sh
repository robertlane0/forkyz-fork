#!/bin/bash

mkdir ../public

mv ipuz.html ../public/ipuz.html

for file in *.md
do
    target=../public/$(basename -s .md $file).html
    cat header.html > $target
    ../node_modules/showdown/bin/showdown.js makehtml -m -i $file >> $target
    cat footer.html >> $target
done

python3 ../scripts/check-translations.py > ../public/missing_translations.html
