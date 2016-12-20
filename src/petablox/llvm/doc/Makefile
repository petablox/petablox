FILENAME=schema
CMD=latexmk -bibtex

default: main

main: ${FILENAME}.tex
	${CMD} -pdf ${FILENAME}.tex

clean:
	${CMD} -C ${FILENAME}.tex

dist: main
	${CMD} -c ${FILENAME}.tex
