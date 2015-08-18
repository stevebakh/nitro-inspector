# Inspect Nitro dataset
Dirty, quickly thrown together bit of code to run through Nitro programmes to do some analysis and find the best way to reduce the size of datasets returned in any given query. **I probably won't use this again, I'm just storing it for the sake of posterity.**

The aim was to find an easier way for certain clients to ingest all content, so reducing the number of pages for any resultset would be beneficial (and reduce the chances of the data being updated while the dataset is being paged).

The code isn't pretty.

It was modified a few times to change the way it traversed the dataset, so if I need to use this again, I'll need to refactor it to be a bit more sensible and configurable, perhaps.

This is a long running process so I used `nohup` to background it and capture the output into files which I could then fiddle about with using sed and awk, like so:

```
sed -E -n "s/(.*)'([0-9]+)'(.*)/\2 \1\2\3/p" nohup.out.available |   awk '{ if ( sum != 0 && $2 == "Masterbrand" ) { print $3$4 ": " sum ; sum = 0 } sum += $1 } END { print $3$4 ": " sum  }'
```

Funsies.
