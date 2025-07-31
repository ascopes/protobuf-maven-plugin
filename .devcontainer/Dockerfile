# syntax=docker/dockerfile:labs
FROM mcr.microsoft.com/devcontainers/base:debian

RUN <<-SCRIPT
  set -o errexit
  rm -Rf ~/.oh-my-zsh

  apt update
  apt full-upgrade -qy
  apt install -qy \
      bash \
      curl \
      git \
      nano \
      protobuf-compiler \
      python3 \
      tmux \
      vim \
      zplug \
      zsh
  chsh -s "$(command -v zsh)"

  # Set up ohmyzsh and change the shell to zsh
  sh -c "$(curl -fsSL https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"
SCRIPT

SHELL ["/usr/bin/env", "zsh", "-c"]

# Configure sdkman.
RUN <<-SCRIPT
  set -o errexit
  curl -s "https://get.sdkman.io" | bash

  set +o errexit
  source /root/.sdkman/bin/sdkman-init.sh
  set -o errexit
  
  # Install java tooling we need
  sdk install java
  sdk install maven
  sdk install mvnd
SCRIPT
