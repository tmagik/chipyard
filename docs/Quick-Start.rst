Quick Start
===============================

Setting up the Chipyard Repo
-------------------------------------------

Start by fetching Chipyard's sources. Run:

.. code-block:: shell

    git clone https://github.com/ucb-bar/chipyard.git
    cd chipyard
    ./scripts/init-submodules-no-riscv-tools.sh

This will have initialized the git submodules.

Installing the RISC-V Tools
-------------------------------------------

We need to install the RISC-V toolchain in order to be able to run RISC-V programs using the Chipyard infrastructure.
This will take about 20-30 minutes. You can expedite the process by setting a ``make`` environment variable to use parallel cores: ``export MAKEFLAGS=-j8``.
To build the toolchains, you should run:

.. code-block:: shell

    ./scripts/build-toolchains.sh

.. Note:: If you are planning to use the Hwacha vector unit, or other RoCC-based accelerators, you should build the esp-tools toolchains by adding the ``esp-tools`` argument to the script above.
  If you are running on an Amazon Web Services EC2 instance, intending to use FireSim, you can also use the ``--ec2fast`` flag for an expedited installation of a pre-compiled toolchain.


What's Next?
-------------------------------------------

This depends on what you are planning to do with Chipyard.

* If you want to learn about the structure of Chipyard, go to :ref:`chipyard-components`.

* If you intend to build one of the vanilla Chipyard examples, go to :ref:`build-a-chip` and follow the instructions.

* If you intend to add a new accelerator, go to :ref:`adding-an-accelerator` and follow the instructions.

* If you intend to run a simulation of one of the vanilla Chipyard examples, go to :ref:`sw-rtl-sim-intro` and follow the instructions.

* If you intend to run a simulation of a custom Chipyard SoC Configuration, go to <> and follow the instructions.

* If you intend to run a full-system FireSim simulation, go to :ref:`firesim-sim-intro` and follow the instructions.

* If you intend to run a VLSI flow using one of the vanilla Chipyard examples, go to <> and follow the instructions.
