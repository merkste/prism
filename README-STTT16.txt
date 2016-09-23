This README provides details for the implementation presented in the
"Advances in Symbolic Model Checking with PRISM" submission to the
special TACAS'16 issue of STTT.

In case of problems / questions, feel free to contact
klein@tcs.inf.tu-dresden.de (perhaps using a throw-away email account
to maintain anonymity).


General information
-------------------

Our implementation for the enhancements presented in the TACAS'16 submission
is based on the model checker PRISM (www.prismmodelchecker.org). We assume here
a basic familarity with PRISM. For general information about PRISM
and its use, please see the website / documentation.

Our version is built upon a recent the PRISM trunk version 

https://github.com/prismmodelchecker/prism-svn/commit/93c2138383f4569d7c1d69d5f55c7fe6224ce703

to profit from the latest bug fixes and also some infrastructure changes that were already
integrated into PRISM by us.

In addition to the enhancements detailed in the article, the version contained here
also contains several additional infrastructure improvements / refactorings.

We are intending to integrate our enhancements into the main PRISM release
in the near future.


Building PRISM
--------------

Our version of PRISM can be built (Linux, OS X) by doing:

cd prism
make

This requires an installed Java JDK (at least version 8) and C/C++ compiler.
If you have trouble compiling our version, please try as well to compile
the latest source version of PRISM from the PRISM home page.


Basics
------

To verify that PRISM is compiled correctly, you can try

prism/bin/prism prism-examples/dice/dice.pm 

from the directory where this README resides. dice.pm is the model file
(Knuth's dice algorithm).

You can also directly provide a query using the -pf switch, e.g.,

prism/bin/prism prism-examples/dice/dice.pm -pf 'P=?[ F s=7 ]'

"what is the probability of eventually reaching s=7"

The queries can also be provided by giving a second file on the command-line.


The PRISM engine can be switched by using
 -explicit   -hybrid   -sparse  -mtbdd
as a parameter.

To set the Java memory limit, use
  -javamaxmem 2g
for a 2GB limit, for CUDD use
  -cuddmaxmem 2g



We will now detail the syntax for our enhancements presented in the paper.

Reordering
----------

For reordering, you can use the following command line switches:

-reorder
-reorder -globalizevariables
-reorder -explodebits
-reorder -globalizevariables -explodebits

The additional option

-reordermaxgrowth 2

specifies a max growth factor of 2 (the MTBDD size is allowed to double during sifting).

Example:

prism/bin/prism -reorder prism-examples/dice/dice.pm

To export the reordered model, use

prism/bin/prism -reorder prism-examples/dice/dice.pm -exportreordered stdout

This will print the reordered model to the console, replace 'stdout' by a filename for
storing to a file.

You can view the result of the -explodebits and -globalizevariables options
(without reordering) by using

prism/bin/prism prism-examples/dice/dice.pm -explodebits -exportprism stdout
prism/bin/prism prism-examples/dice/dice.pm -explodebits -globalizevariables -exportprism stdout

You can use option 
 -reorderoptions beforereach
to perform reordering before the transition matrix is restricted to the reachable part of the
state space.



(Multi-)reward bounded properties
---------------------------------

We exemplify the syntax for reward-bounded properties here for the P=? operator, it also works for
Pmax and Pmin.

  P=?[ F<3 "goal" ]                     standard step bound <3
  P=?[ F{steps<3} "goal" ]              extended syntax: step bound <3
  P=?[ F{reward{"rounds"}<3} "goal" ]   reward bound (structure "rounds") <3

Conjunctions are specified by providing multiple bounds (comma-separated):

  P=?[ F{reward{"rounds"}<3,steps<6} "goal" ]  reward bound and step bound


Example:

prism/bin/prism prism-examples/dice/dice.pm -pf 'P=?[ F{reward{"coin_flips"}>5,steps<=8} s=7 ]'

"What is the probability of reaching the end state (s=7) after more than 5 coin flips, but in at most 8 steps"

To not use the quantile backend but treat all the reward bounds via a counter product, you can
use the option -boundsviacounters.


Quantiles
---------

We now detail the syntax for quantile queries (for MDPs, as they are the most interesting
case for quantiles).

 quantile( min r, Pmax>0.5 [ F{reward{"rounds}<=r} "goal"] )

would specify the quantile "the minimal number of rounds r that is needed to ensure reaching
a state satisfying the goal label". Other combinations of min r / max r, Pmin/Pmax and
upper and lower bounds in the F operator are supported as well. Some combinations do not make sense
and for some, only strict / non-strict probability bounds are supported. To get a feel for the
quantile queries, there's an interactive tool at 
 http://wwwtcs.inf.tu-dresden.de/~klein/quantiles/quantile.html

It is possible to specify multiple thresholds, which can be calculated in a single run, i.e.,
   quantile( min r, Pmax>{0.1,0.5,0.9} [ F{reward{"rounds}<=r} "goal"] )

Example:

prism/bin/prism prism-examples/dice/two_dice.nm -pf 'quantile( min r, Pmax>0.5 [ F{reward{"coin_flips"}<=r} s1=7] )'

"What is the minimal number of coin flips that is needed for the best scheduler to ensure that process 1
 is done (s=7) with probability at least 0.5"

The implementation of the symbolic quantile engines has been improved since the TACAS'16 version as detailed in the
article. You can still access the previous version using the -quantileTACAS16 flag.


Expectations for co-safety LTL
------------------------------

You can simple specify a property of the form

 R=?[ X X X "goal" ]

i.e., with a complex LTL formula inside the [ ] instead of the "standard" PRISM reward operators.

Some examples can be found in the prism test suite, e.g., here:

https://github.com/prismmodelchecker/prism-tests/tree/d88ae235067d9b58fdfcf1a2fd61972a0a0c6565/functionality/verify/mdps/rewards/cosafe/


