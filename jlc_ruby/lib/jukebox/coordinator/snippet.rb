
module Jukebox
  class Coordinator
    class Snippet
      attr_accessor :expression

      def initialize(expression)
        @expression = expression
      end

      ARGUMENT_PATTERNS = ['"([^"]*)"', '(\d+)'].freeze

      def parameter_count()
        modified_pattern = ::Regexp.escape(@expression).gsub('\ ', ' ').gsub('/', '\/')
        number_of_arguments = 0

        ARGUMENT_PATTERNS.each do |argument_pattern|
          number_of_arguments += modified_pattern.scan(argument_pattern).length
        end

        number_of_arguments
      end

      def parameters(number_of_arguments, separator)
        block_args = (0...number_of_arguments).map { |n| "arg#{n + 1}" }
        # multiline_argument.append_block_parameter_to(block_args)
        block_args.empty? ? 'board' : "board#{separator}#{block_args.join(separator)}"
      end

      def render(argument_joiner:, escape_pattern:, template:)
        template.sub(/\{1}/, @expression)
                .sub(/\{3}/, parameters(parameter_count, argument_joiner))
                .sub(/\{4}/, 'Write code here that turns the phrase above into concrete actions')
      end
    end
  end
end
