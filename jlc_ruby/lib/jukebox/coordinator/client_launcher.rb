# frozen_string_literal: true

module Jukebox
  class Coordinator
    class ClientLauncher
      class <<self
        attr_reader :launchers
      end

      def inherited(launcher)
        launchers << launcher
      end
    end
  end
end
