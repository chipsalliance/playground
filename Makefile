# registe .gitmodule to local configurations
init:
	cd dependencies
	# clone each repository from .gitmodules if this project is not a git repository
	cat ../.gitmodules|rg -P '(?<=url = ).*' -o|while read repo ; do git clone $repo ; done
	git add dependencies
	git submodule init
	# your sources codes lays here
	mkdir -p playground/src
	touch playground/src/Playground.scala

# update each submodules from dependencies, if you have your own mirrors for submodules(e.g in a internal network), update url .git/config before running update
update:
	git submodule update	

# bump all repositories to master(use it at your own risk)
bump:
	git submodule foreach "git fetch --all && git stash &&git checkout origin/master"

bsp:
	mill -i mill.bsp.BSP/install

compile:
	mill -i playground.compile

clean:
	git clean -fd
