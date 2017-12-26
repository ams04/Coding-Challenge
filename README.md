# Coding-Challenge

Using the translation service described below, create a command line tool in Java to translate an arbitrary number of files 
from English to Swedish. (3 examples provided). The tool should take the names of the files as arguments. Although the example
files are small, the solution should be able to handle arbitrarily large files; that is, assume each input file and output file
may be larger in bytes than the memory allocated to the JVM.  All specified input files should be read and translated concurrently; 
that is, lines from each file should be processed in parallel without being blocked by the progress of the other files or by the batches.
The results should be written to two separate text files output to the current directory. The first should be named AsYouGo.txt and have
all results, one word per line, written as soon as they are processed. The second should be named Batched.txt and have one word per
line in batches. Each batch should contain one translated result from each file, and sorted within the batch in alphabetical order.
The batches should correspond to the original ordering in the files (i.e. the first batch would be a sorted list of the translations
from the 1st line of each file, the second a sorted list from the 2nd line of each file, and so on).  Example:

Given the following files:

File1:
one
two
three

File2: 
four
five
six

File3:
seven
eight
nine

If the translations are:

File1:
en
två
tre

File2:
fyra
fem
sex

File3:
sju
åtta
nio

Then the batched result would be:

en
fyra
sju
fem
två
åtta
nio
sex
tre

Translate Service URL: 
	
	https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=sv&dt=t&q={toTranslate}

Notes on the API: 
- The deficiencies of this endpoint are part of the exercise. 
- The URL already includes the parameters for English to Swedish
- It will temporarily block usage if it is used too much so test carefully. 
- It is legitimate for the translation to return the original word in cases where it's not found. In those cases the original should be used in the output.
