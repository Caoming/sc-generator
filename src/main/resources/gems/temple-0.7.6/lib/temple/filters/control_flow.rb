module Temple
  module Filters
    # Control flow filter which processes [:if, condition, yes-exp, no-exp]
    # and [:block, code, content] expressions.
    # This is useful for ruby code generation with lots of conditionals.
    #
    # @api public
    class ControlFlow < Filter
      def on_if(condition, yes, no = nil)
        result = [:multi, [:sc, "if #{condition}"], compile(yes)]
        while no && no.first == :if
          result << [:sc, "elsif #{no[1]}"] << compile(no[2])
          no = no[3]
        end
        result << [:sc, 'else'] << compile(no) if no
        result << [:sc, 'end']
        result
      end

      def on_case(arg, *cases)
        result = [:multi, [:sc, arg ? "case (#{arg})" : 'case']]
        cases.map do |c|
          condition, exp = c
          result << [:sc, condition == :else ? 'else' : "when #{condition}"] << compile(exp)
        end
        result << [:sc, 'end']
        result
      end

      def on_cond(*cases)
        on_case(nil, *cases)
      end

      def on_block(code, exp)
        [:multi,
         [:sc, code],
         compile(exp),
         [:sc, 'end']]
      end
    end
  end
end
